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

import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.managers.Custom

/*
 * Centralized reflective accessors for package-private remote-core operation fields the embedded
 * player needs to read (remote-core left unchanged — no public getters yet). Cached `Field`s,
 * resolved once at class load. Every (class, field) pair here is asserted to exist by
 * `CoreReflectionGuardTest`, so a core rename fails the build rather than the field.
 *
 * (A handful of accessors local to a single modifier/layout — e.g. HostNamedActionOperation in
 * ClickModifier, mSpacedBy in Row/Column — remain at their call sites; they're also covered by the
 * guard test.)
 */

/** [ClipPath]'s package-private fields. */
internal val clipPathIdField =
    ClipPath::class.java.getDeclaredField("mId").apply { isAccessible = true }
internal val clipPathRegionOpField =
    ClipPath::class.java.getDeclaredField("mRegionOp").apply { isAccessible = true }

/** [DrawTextOnPath]'s package-private fields. */
internal val dtopPathIdField =
    DrawTextOnPath::class.java.getDeclaredField("mPathId").apply { isAccessible = true }
internal val dtopOutHOffsetField =
    DrawTextOnPath::class.java.getDeclaredField("mOutHOffset").apply { isAccessible = true }
internal val dtopOutVOffsetField =
    DrawTextOnPath::class.java.getDeclaredField("mOutVOffset").apply { isAccessible = true }

/** [DrawTextAnchored]'s package-private fields. */
internal val dtaTextIdField =
    DrawTextAnchored::class.java.getDeclaredField("mTextID").apply { isAccessible = true }
internal val dtaOutXField =
    DrawTextAnchored::class.java.getDeclaredField("mOutX").apply { isAccessible = true }
internal val dtaOutYField =
    DrawTextAnchored::class.java.getDeclaredField("mOutY").apply { isAccessible = true }
internal val dtaOutPanXField =
    DrawTextAnchored::class.java.getDeclaredField("mOutPanX").apply { isAccessible = true }
internal val dtaOutPanYField =
    DrawTextAnchored::class.java.getDeclaredField("mOutPanY").apply { isAccessible = true }
internal val dtaFlagsField =
    DrawTextAnchored::class.java.getDeclaredField("mFlags").apply { isAccessible = true }

/** [DrawBitmapInt]'s package-private fields. */
internal val dbiImageIdField =
    DrawBitmapInt::class.java.getDeclaredField("mImageId").apply { isAccessible = true }
internal val dbiSrcLeftField =
    DrawBitmapInt::class.java.getDeclaredField("mSrcLeft").apply { isAccessible = true }
internal val dbiSrcTopField =
    DrawBitmapInt::class.java.getDeclaredField("mSrcTop").apply { isAccessible = true }
internal val dbiSrcRightField =
    DrawBitmapInt::class.java.getDeclaredField("mSrcRight").apply { isAccessible = true }
internal val dbiSrcBottomField =
    DrawBitmapInt::class.java.getDeclaredField("mSrcBottom").apply { isAccessible = true }
internal val dbiDstLeftField =
    DrawBitmapInt::class.java.getDeclaredField("mDstLeft").apply { isAccessible = true }
internal val dbiDstTopField =
    DrawBitmapInt::class.java.getDeclaredField("mDstTop").apply { isAccessible = true }
internal val dbiDstRightField =
    DrawBitmapInt::class.java.getDeclaredField("mDstRight").apply { isAccessible = true }
internal val dbiDstBottomField =
    DrawBitmapInt::class.java.getDeclaredField("mDstBottom").apply { isAccessible = true }

/** [DrawTextOnCircle]'s package-private geometry fields. */
internal val dtocCenterXField =
    DrawTextOnCircle::class.java.getDeclaredField("mCenterX").apply { isAccessible = true }
internal val dtocCenterYField =
    DrawTextOnCircle::class.java.getDeclaredField("mCenterY").apply { isAccessible = true }
internal val dtocRadiusField =
    DrawTextOnCircle::class.java.getDeclaredField("mRadius").apply { isAccessible = true }
internal val dtocStartAngleField =
    DrawTextOnCircle::class.java.getDeclaredField("mStartAngle").apply { isAccessible = true }
internal val dtocWarpRadiusOffsetField =
    DrawTextOnCircle::class.java.getDeclaredField("mWarpRadiusOffset").apply { isAccessible = true }
internal val dtocAlignmentField =
    DrawTextOnCircle::class.java.getDeclaredField("mAlignment").apply { isAccessible = true }
internal val dtocPlacementField =
    DrawTextOnCircle::class.java.getDeclaredField("mPlacement").apply { isAccessible = true }

/** [DrawBitmapFontText]'s package-private fields + the font's kerning table. */
internal val dbftTextIdField =
    DrawBitmapFontText::class.java.getDeclaredField("mTextID").apply { isAccessible = true }
internal val dbftFontIdField =
    DrawBitmapFontText::class.java.getDeclaredField("mBitmapFontID").apply { isAccessible = true }
internal val dbftStartField =
    DrawBitmapFontText::class.java.getDeclaredField("mStart").apply { isAccessible = true }
internal val dbftEndField =
    DrawBitmapFontText::class.java.getDeclaredField("mEnd").apply { isAccessible = true }
internal val dbftOutXField =
    DrawBitmapFontText::class.java.getDeclaredField("mOutX").apply { isAccessible = true }
internal val dbftOutYField =
    DrawBitmapFontText::class.java.getDeclaredField("mOutY").apply { isAccessible = true }
internal val dbftOutGlyphSpacingField =
    DrawBitmapFontText::class.java.getDeclaredField("mOutGlyphSpacing").apply { isAccessible = true }

internal val bitmapFontKerningField =
    BitmapFontData::class.java.getDeclaredField("mKerningTable").apply { isAccessible = true }

/**
 * [DrawBitmapFontTextOnPath]'s package-private fields. Lays a bitmap-font run along a path: each
 * glyph is positioned + rotated by a path matrix at its mid-run fraction. `mOutYAdj`/
 * `mOutGlyphSpacing` are resolved by `updateVariables`.
 */
internal val dbfopTextIdField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mTextID").apply { isAccessible = true }
internal val dbfopFontIdField =
    DrawBitmapFontTextOnPath::class.java
        .getDeclaredField("mBitmapFontID")
        .apply { isAccessible = true }
internal val dbfopPathIdField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mPathID").apply { isAccessible = true }
internal val dbfopStartField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mStart").apply { isAccessible = true }
internal val dbfopEndField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mEnd").apply { isAccessible = true }
internal val dbfopOutYAdjField =
    DrawBitmapFontTextOnPath::class.java.getDeclaredField("mOutYAdj").apply { isAccessible = true }
internal val dbfopOutGlyphSpacingField =
    DrawBitmapFontTextOnPath::class.java
        .getDeclaredField("mOutGlyphSpacing")
        .apply { isAccessible = true }

/**
 * [DrawBitmapTextAnchored]'s package-private fields. Draws a bitmap-font run anchored about
 * (mOutX, mOutY) with pan in [-1, 1]; `mOut*` are resolved by `updateVariables`.
 */
internal val dbtaTextIdField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mTextID").apply { isAccessible = true }
internal val dbtaFontIdField =
    DrawBitmapTextAnchored::class.java
        .getDeclaredField("mBitmapFontID")
        .apply { isAccessible = true }
internal val dbtaOutStartField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutStart").apply { isAccessible = true }
internal val dbtaOutEndField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutEnd").apply { isAccessible = true }
internal val dbtaOutXField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutX").apply { isAccessible = true }
internal val dbtaOutYField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutY").apply { isAccessible = true }
internal val dbtaOutPanXField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutPanX").apply { isAccessible = true }
internal val dbtaOutPanYField =
    DrawBitmapTextAnchored::class.java.getDeclaredField("mOutPanY").apply { isAccessible = true }
internal val dbtaOutGlyphSpacingField =
    DrawBitmapTextAnchored::class.java
        .getDeclaredField("mOutGlyphSpacing")
        .apply { isAccessible = true }

/** [DrawToBitmap]'s private fields. */
internal val dtbBitmapIdField =
    DrawToBitmap::class.java.getDeclaredField("mBitmapId").apply { isAccessible = true }
internal val dtbModeField =
    DrawToBitmap::class.java.getDeclaredField("mMode").apply { isAccessible = true }
internal val dtbColorField =
    DrawToBitmap::class.java.getDeclaredField("mColor").apply { isAccessible = true }

/** [DrawTweenPath]'s package-private fields. */
internal val dtpPath1IdField =
    DrawTweenPath::class.java.getDeclaredField("mPath1Id").apply { isAccessible = true }
internal val dtpPath2IdField =
    DrawTweenPath::class.java.getDeclaredField("mPath2Id").apply { isAccessible = true }
internal val dtpOutTweenField =
    DrawTweenPath::class.java.getDeclaredField("mOutTween").apply { isAccessible = true }
internal val dtpOutStartField =
    DrawTweenPath::class.java.getDeclaredField("mOutStart").apply { isAccessible = true }
internal val dtpOutStopField =
    DrawTweenPath::class.java.getDeclaredField("mOutStop").apply { isAccessible = true }

/**
 * [ParticlesLoop]'s package-private engine state. The Compose player drives the per-frame simulation
 * itself (loop + draw uplifted to Compose), reusing the core's resolved equations + evaluator rather
 * than the core's PaintContext `paint()`. The init/eval *math* is reused via public methods
 * (`updateVariables`, `AnimatedFloatExpression.eval`, `ParticlesCreate.initializeParticle`); these
 * fields expose the resolved equation arrays + evaluator + source that those need.
 */
internal val plOutEquationsField =
    ParticlesLoop::class.java.getDeclaredField("mOutEquations").apply { isAccessible = true }
internal val plExpField =
    ParticlesLoop::class.java.getDeclaredField("mExp").apply { isAccessible = true }
internal val plOutRestartField =
    ParticlesLoop::class.java.getDeclaredField("mOutRestart").apply { isAccessible = true }
internal val plSourceField =
    ParticlesLoop::class.java.getDeclaredField("mParticlesSource").apply { isAccessible = true }

/** [ParticlesCreate.initializeParticle] — package-private; (re)seeds one particle's state. */
internal val pcInitializeParticleMethod =
    ParticlesCreate::class
        .java
        .getDeclaredMethod("initializeParticle", Int::class.javaPrimitiveType)
        .apply { isAccessible = true }

/**
 * [ParticlesCompare]'s package-private engine state. Like [ParticlesLoop], the per-particle (or
 * pairwise) comparison + child draw is uplifted to Compose, reusing the resolved RPN equation arrays
 * + the [AnimatedFloatExpression] evaluator (the *math*) rather than the core's PaintContext
 * `paint()`. The source provides the particle array + variable ids (public getters).
 */
internal val pcmpSourceField =
    ParticlesCompare::class.java.getDeclaredField("mParticlesSource").apply { isAccessible = true }
internal val pcmpExpField =
    ParticlesCompare::class.java.getDeclaredField("mExp").apply { isAccessible = true }
internal val pcmpExpressionField =
    ParticlesCompare::class.java.getDeclaredField("mExpression").apply { isAccessible = true }
internal val pcmpOutExpressionField =
    ParticlesCompare::class.java.getDeclaredField("mOutExpression").apply { isAccessible = true }
internal val pcmpEquations1Field =
    ParticlesCompare::class.java.getDeclaredField("mEquations1").apply { isAccessible = true }
internal val pcmpOutEquations1Field =
    ParticlesCompare::class.java.getDeclaredField("mOutEquations1").apply { isAccessible = true }
internal val pcmpEquations2Field =
    ParticlesCompare::class.java.getDeclaredField("mEquations2").apply { isAccessible = true }
internal val pcmpOutEquations2Field =
    ParticlesCompare::class.java.getDeclaredField("mOutEquations2").apply { isAccessible = true }
internal val pcmpOutMinField =
    ParticlesCompare::class.java.getDeclaredField("mOutMin").apply { isAccessible = true }
internal val pcmpOutMaxField =
    ParticlesCompare::class.java.getDeclaredField("mOutMax").apply { isAccessible = true }

/** [ConditionalOperations]'s resolved comparison operands + type (drives whether children draw). */
internal val condAOutField =
    ConditionalOperations::class.java.getDeclaredField("mVarAOut").apply { isAccessible = true }
internal val condBOutField =
    ConditionalOperations::class.java.getDeclaredField("mVarBOut").apply { isAccessible = true }
internal val condTypeField =
    ConditionalOperations::class.java.getDeclaredField("mType").apply { isAccessible = true }

/**
 * [LoopOperation]'s resolved bounds + index variable. The Compose player runs the loop itself
 * (recursing into the child list per iteration), reusing the core's `updateVariables` to resolve the
 * NaN-id bounds; these fields expose the resolved `from`/`until`/`step` + the index variable id.
 */
internal val loopFromOutField =
    LoopOperation::class.java.getDeclaredField("mFromOut").apply { isAccessible = true }
internal val loopUntilOutField =
    LoopOperation::class.java.getDeclaredField("mUntilOut").apply { isAccessible = true }
internal val loopStepOutField =
    LoopOperation::class.java.getDeclaredField("mStepOut").apply { isAccessible = true }
internal val loopIndexVarField =
    LoopOperation::class.java.getDeclaredField("mIndexVariableId").apply { isAccessible = true }

/**
 * [FloatFunctionCall]'s resolved function + arguments. The resolved [FloatFunctionDefine]
 * (`mFunction`, wired during `registerListening` at setup) and the resolved argument values
 * (`mOutArgs`, filled by `updateVariables`) are needed to load the function's arg variables and
 * invoke its public `execute`.
 */
internal val ffcFunctionField =
    FloatFunctionCall::class.java.getDeclaredField("mFunction").apply { isAccessible = true }
internal val ffcOutArgsField =
    FloatFunctionCall::class.java.getDeclaredField("mOutArgs").apply { isAccessible = true }

/** [Custom]'s package-private config + properties (the host-extension component descriptor). */
internal val customConfigField =
    Custom::class.java.getDeclaredField("mConfig").apply { isAccessible = true }
internal val customConfigIdField =
    Custom::class.java.getDeclaredField("mConfigId").apply { isAccessible = true }
internal val customPropertiesField =
    Custom::class.java.getDeclaredField("mProperties").apply { isAccessible = true }
