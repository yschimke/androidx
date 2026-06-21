/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.NanMap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draws a [ParticlesLoop] — the embedded player's Compose-native particle engine.
 *
 * Rather than driving the core's `paint()` (which is tied to the View player's PaintContext draw
 * model), the loop and per-frame stepping live here in Compose; only the *math* is reused from
 * remote-core — the resolved RPN equations and the [AnimatedFloatExpression] evaluator, the same way
 * the derived-value graph reuses ops' `updateVariables`/`apply`. Each frame:
 *  - seed the particles once (`ParticlesCreate.initializeParticle`, reused),
 *  - step every particle: load its dimensions into the store, re-resolve the update equations
 *    (`updateVariables`, reused), evaluate them, and re-seed if the restart equation fires,
 *  - draw the loop's child ops once per particle via [executeOperations], so they render at each
 *    particle's current position (the children read the particle dimensions back out of the store).
 *
 * Continuous animation: reading the frame-clock time through [graph] registers this draw as an
 * observer of it, so it re-runs (and steps) each tick while the player's frame loop is alive (kept
 * alive for documents containing particles).
 *
 * State (the float[count][dims] particle array) lives on the core [ParticlesCreate] op and is
 * advanced in place; the one-time seeding is tracked per document on [GraphContext].
 */
internal fun DrawScope.drawParticles(
    loop: ParticlesLoop,
    remoteContext: RemoteContext,
    paintState: ComposeLocalPaint,
    graph: GraphContext,
    onDrawContent: () -> Unit,
) {
    val source = plSourceField.get(loop) as? ParticlesCreate ?: return
    val particles = source.particles
    val varIds = source.variableIds
    if (particles.isEmpty() || varIds.isEmpty()) return

    // Seed once per document. updateVariables resolves the source's initial equations first.
    if (graph.particlesInitialized.add(System.identityHashCode(loop))) {
        source.updateVariables(remoteContext)
        for (i in particles.indices) pcInitializeParticleMethod.invoke(source, i)
    }

    // Observe the frame clock so the draw re-runs each tick (continuous simulation).
    graph.getFloat(RemoteContext.ID_TIME_IN_SEC)

    loop.updateVariables(remoteContext)
    @Suppress("UNCHECKED_CAST") val outEquations = plOutEquationsField.get(loop) as Array<FloatArray>
    val exp = plExpField.get(loop) as AnimatedFloatExpression
    val outRestart = plOutRestartField.get(loop) as? FloatArray
    val collections = remoteContext.collectionsAccess ?: return
    val children = loop.list

    for (i in particles.indices) {
        val particle = particles[i]
        // Load the particle's current dimensions, then re-resolve the update/restart equations
        // against them.
        for (j in particle.indices) remoteContext.loadFloat(varIds[j], particle[j])
        loop.updateVariables(remoteContext)
        for (j in particle.indices) {
            particle[j] = exp.eval(collections, outEquations[j], outEquations[j].size)
            remoteContext.loadFloat(varIds[j], particle[j])
        }
        if (outRestart != null && exp.eval(collections, outRestart, outRestart.size) > 0f) {
            pcInitializeParticleMethod.invoke(source, i)
        }
        // Draw this particle's instance of the child ops at its current dimensions.
        executeOperations(children, remoteContext, paintState, onDrawContent, graph)
    }
}

/** A NaN value that is a plain variable reference (not a math operator nor a data variable). */
private fun isVariableRef(v: Float): Boolean =
    v.isNaN() && !AnimatedFloatExpression.isMathOperator(v) && !NanMap.isDataVariable(v)

/** Resolve a single RPN array's variable refs into [out]. Mirrors `ParticlesCompare.update`. */
private fun resolveExpression(context: RemoteContext, expression: FloatArray?, out: FloatArray?) {
    if (expression == null || out == null) return
    for (i in expression.indices) {
        val v = expression[i]
        out[i] = if (isVariableRef(v)) context.getFloat(Utils.idFromNan(v)) else v
    }
}

/** Resolve each equation's variable refs into [out]. Mirrors `ParticlesCompare.update` (2D). */
private fun resolveEquations(
    context: RemoteContext,
    equations: Array<FloatArray>?,
    out: Array<FloatArray>?,
) {
    if (equations == null || out == null) return
    for (i in equations.indices) {
        val eq = equations[i]
        for (j in eq.indices) {
            val v = eq[j]
            out[i][j] = if (isVariableRef(v)) context.getFloat(Utils.idFromNan(v)) else v
        }
    }
}

/**
 * Resolve [expression] into [out], substituting the CMD1/CMD2 markers that follow a particle-variable
 * id with that variable's value from [particle1]/[particle2]. Mirrors `ParticlesCompare.update2Body`.
 */
private fun resolvePairExpression(
    context: RemoteContext,
    expression: FloatArray?,
    out: FloatArray?,
    varIds: IntArray,
    particle1: FloatArray,
    particle2: FloatArray,
) {
    if (expression == null || out == null) return
    var i = 0
    while (i < expression.size) {
        val v = expression[i]
        out[i] = if (isVariableRef(v)) context.getFloat(Utils.idFromNan(v)) else v
        if (v.isNaN() && i + 1 < expression.size) {
            for (k in varIds.indices) {
                if (Utils.idFromNan(v) == varIds[k]) {
                    when (expression[i + 1].toRawBits()) {
                        AnimatedFloatExpression.CMD1.toRawBits() -> {
                            out[i] = particle1[k]
                            out[i + 1] = AnimatedFloatExpression.NOP
                            i++
                        }
                        AnimatedFloatExpression.CMD2.toRawBits() -> {
                            out[i] = particle2[k]
                            out[i + 1] = AnimatedFloatExpression.NOP
                            i++
                        }
                    }
                }
            }
        }
        i++
    }
}

/** As [resolvePairExpression] but over equation rows, with a [reverse] default. 2D update2Body. */
private fun resolvePairEquations(
    context: RemoteContext,
    equations: Array<FloatArray>?,
    out: Array<FloatArray>?,
    varIds: IntArray,
    particle1: FloatArray,
    particle2: FloatArray,
    reverse: Boolean,
) {
    if (equations == null || out == null) return
    for (row in equations.indices) {
        val eq = equations[row]
        var j = 0
        while (j < eq.size) {
            val v = eq[j]
            out[row][j] = if (isVariableRef(v)) context.getFloat(Utils.idFromNan(v)) else v
            if (v.isNaN() && j + 1 < eq.size) {
                for (k in varIds.indices) {
                    if (Utils.idFromNan(v) == varIds[k]) {
                        when (eq[j + 1].toRawBits()) {
                            AnimatedFloatExpression.CMD1.toRawBits() -> {
                                out[row][j] = particle1[k]
                                out[row][j + 1] = AnimatedFloatExpression.NOP
                                j++
                            }
                            AnimatedFloatExpression.CMD2.toRawBits() -> {
                                out[row][j] = particle2[k]
                                out[row][j + 1] = AnimatedFloatExpression.NOP
                                j++
                            }
                            else -> out[row][j] = if (reverse) particle2[k] else particle1[k]
                        }
                    }
                }
            }
            j++
        }
    }
}

/** Load each particle dimension into its variable id. Mirrors `setupForParticle`. */
private fun setupForParticle(context: RemoteContext, particle: FloatArray, varIds: IntArray) {
    for (j in particle.indices) context.loadFloat(varIds[j], particle[j])
}

/**
 * Draws a [ParticlesCompare] — the embedded player's Compose-native particle-interaction pass.
 *
 * Like [drawParticles], the loop + per-particle simulation is uplifted to Compose, reusing only the
 * resolved RPN equations + the [AnimatedFloatExpression] evaluator. ParticlesCompare evolves an
 * already-seeded particle set (it does not seed): it walks particles in the [min, max] window,
 * evaluates a comparison expression, and — when it fires — runs update equations to mutate the
 * particle(s) and draws the child ops. With two equation blocks it runs the pairwise body (each pair
 * once, mutating both particles); with one, the per-particle body. Mirrors `ParticlesCompare.paint`.
 */
internal fun DrawScope.drawParticlesCompare(
    op: ParticlesCompare,
    remoteContext: RemoteContext,
    paintState: ComposeLocalPaint,
    graph: GraphContext,
    onDrawContent: () -> Unit,
) {
    val source = pcmpSourceField.get(op) as? ParticlesCreate ?: return
    val particles = source.particles
    val varIds = source.variableIds
    if (particles.isEmpty() || varIds.isEmpty()) return

    // Observe the frame clock so the draw re-runs each tick (continuous simulation).
    graph.getFloat(RemoteContext.ID_TIME_IN_SEC)

    op.updateVariables(remoteContext)
    val exp = pcmpExpField.get(op) as AnimatedFloatExpression
    val collections = remoteContext.collectionsAccess ?: return
    val children = op.list
    val outMin = pcmpOutMinField.getFloat(op)
    val outMax = pcmpOutMaxField.getFloat(op)
    val start = if (outMin < 0f) 0 else outMin.toInt()
    val end = if (outMax < 0f) particles.size else outMax.toInt().coerceAtMost(particles.size)

    val expression = pcmpExpressionField.get(op) as? FloatArray
    val outExpression = pcmpOutExpressionField.get(op) as? FloatArray
    @Suppress("UNCHECKED_CAST")
    val equations1 = pcmpEquations1Field.get(op) as? Array<FloatArray>
    @Suppress("UNCHECKED_CAST")
    val outEquations1 = pcmpOutEquations1Field.get(op) as? Array<FloatArray>
    @Suppress("UNCHECKED_CAST")
    val equations2 = pcmpEquations2Field.get(op) as? Array<FloatArray>
    @Suppress("UNCHECKED_CAST")
    val outEquations2 = pcmpOutEquations2Field.get(op) as? Array<FloatArray>

    if (
        equations1 != null && equations2 != null && outEquations1 != null && outEquations2 != null
    ) {
        // condition2Body: each pair (k, i>k) once; mutate both particles, draw children after each.
        for (k in start until end) {
            val particle2 = particles[k]
            for (i in (k + 1) until end) {
                val particle1 = particles[i]
                setupForParticle(remoteContext, particle1, varIds)
                resolvePairExpression(
                    remoteContext,
                    expression,
                    outExpression,
                    varIds,
                    particle1,
                    particle2,
                )
                val value =
                    if (outExpression == null) 0f
                    else exp.eval(collections, outExpression, outExpression.size)
                if (value > 0f) {
                    resolvePairEquations(
                        remoteContext,
                        equations1,
                        outEquations1,
                        varIds,
                        particle1,
                        particle2,
                        false,
                    )
                    setupForParticle(remoteContext, particle2, varIds)
                    resolvePairEquations(
                        remoteContext,
                        equations2,
                        outEquations2,
                        varIds,
                        particle1,
                        particle2,
                        true,
                    )
                    for (j in particle1.indices) {
                        particle1[j] = exp.eval(collections, outEquations1[j], outEquations1[j].size)
                        remoteContext.loadFloat(varIds[j], particle1[j])
                    }
                    executeOperations(children, remoteContext, paintState, onDrawContent, graph)
                    for (j in particle2.indices) {
                        particle2[j] = exp.eval(collections, outEquations2[j], outEquations2[j].size)
                        remoteContext.loadFloat(varIds[j], particle2[j])
                    }
                    executeOperations(children, remoteContext, paintState, onDrawContent, graph)
                }
            }
        }
    } else {
        // condition1Body: per-particle; mutate the particle and draw children when the test fires.
        if (outExpression == null) return
        for (i in start until end) {
            val particle = particles[i]
            setupForParticle(remoteContext, particle, varIds)
            resolveExpression(remoteContext, expression, outExpression)
            val value1 = exp.eval(collections, outExpression, outExpression.size)
            if (value1 > 0f && outEquations1 != null) {
                resolveEquations(remoteContext, equations1, outEquations1)
                for (j in particle.indices) {
                    particle[j] = exp.eval(collections, outEquations1[j], outEquations1[j].size)
                    remoteContext.loadFloat(varIds[j], particle[j])
                }
                executeOperations(children, remoteContext, paintState, onDrawContent, graph)
            }
        }
    }
}
