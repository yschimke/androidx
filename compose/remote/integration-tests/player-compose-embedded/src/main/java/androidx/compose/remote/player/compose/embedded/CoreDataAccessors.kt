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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation

/*
 * Centralized accessors for the remote-core operation state the embedded player needs to read,
 * encapsulated via Kotlin data classes and extension methods over the core getters
 * (the "*Reflection" names are historical: these used to read package-private fields
 * reflectively before remote-core exposed accessors).
 */

internal fun ClipPath.readData(): ClipPathData {
    return ClipPathData(
        id = getId(),
        regionOp = getRegionOp(),
    )
}

internal fun DrawTextOnPath.readData(): DrawTextOnPathData {
    return DrawTextOnPathData(
        pathId = getPathId(),
        hOffset = getOutHOffset(),
        vOffset = getOutVOffset(),
    )
}

internal fun DrawTextAnchored.readData(): DrawTextAnchoredData {
    return DrawTextAnchoredData(
        textId = getTextID(),
        x = getOutX(),
        y = getOutY(),
        panX = getOutPanX(),
        panY = getOutPanY(),
        flags = getFlags(),
    )
}

internal fun DrawBitmapInt.readData(): DrawBitmapIntData {
    return DrawBitmapIntData(
        imageId = getImageId(),
        srcLeft = getSrcLeft(),
        srcTop = getSrcTop(),
        srcRight = getSrcRight(),
        srcBottom = getSrcBottom(),
        dstLeft = getDstLeft(),
        dstTop = getDstTop(),
        dstRight = getDstRight(),
        dstBottom = getDstBottom(),
    )
}

internal fun DrawTextOnCircle.readData(): DrawTextOnCircleData {
    return DrawTextOnCircleData(
        centerX = getCenterX(),
        centerY = getCenterY(),
        radius = getRadius(),
        startAngle = getStartAngle(),
        warpRadiusOffset = getWarpRadiusOffset(),
        alignment = getAlignment() as DrawTextOnCircle.Alignment,
        placement = getPlacement() as DrawTextOnCircle.Placement,
    )
}

internal fun DrawBitmapFontText.readData(): DrawBitmapFontTextData {
    return DrawBitmapFontTextData(
        textId = getTextID(),
        fontId = getBitmapFontID(),
        start = getStart(),
        end = getEnd(),
        x = getOutX(),
        y = getOutY(),
        glyphSpacing = getOutGlyphSpacing(),
    )
}

internal fun DrawBitmapFontTextOnPath.readData(): DrawBitmapFontTextOnPathData {
    return DrawBitmapFontTextOnPathData(
        textId = getTextID(),
        fontId = getBitmapFontID(),
        pathId = getPathID(),
        start = getStart(),
        end = getEnd(),
        yAdj = getOutYAdj(),
        glyphSpacing = getOutGlyphSpacing(),
    )
}

internal fun DrawBitmapTextAnchored.readData(): DrawBitmapTextAnchoredData {
    return DrawBitmapTextAnchoredData(
        textId = getTextID(),
        fontId = getBitmapFontID(),
        start = getOutStart().toInt(),
        end = getOutEnd().toInt(),
        x = getOutX(),
        y = getOutY(),
        panX = getOutPanX(),
        panY = getOutPanY(),
        glyphSpacing = getOutGlyphSpacing(),
    )
}

internal fun DrawToBitmap.readData(): DrawToBitmapData {
    return DrawToBitmapData(
        bitmapId = getBitmapId(),
        mode = getMode(),
        color = getColor(),
    )
}

internal fun DrawTweenPath.readData(): DrawTweenPathData {
    return DrawTweenPathData(
        path1Id = getPath1Id(),
        path2Id = getPath2Id(),
        tween = getOutTween(),
        start = getOutStart(),
        stop = getOutStop(),
    )
}

/**
 * The [ParticlesCreate] op a [ParticlesLoop] draws from. Particle *rendering* is bridged to the
 * core implementation (see RcPlayerParticles); the player only needs the source to run the core
 * seeding path once per document.
 */
internal val ParticlesLoop.particlesSourceReflection: ParticlesCreate?
    get() = getParticlesSource() as? ParticlesCreate

internal fun ConditionalOperations.readData(): ConditionalOperationsData {
    return ConditionalOperationsData(
        varAOut = getVarAOut(),
        varBOut = getVarBOut(),
        type = getType(),
    )
}

internal fun LoopOperation.readData(): LoopOperationData {
    return LoopOperationData(
        fromOut = getFromOut(),
        untilOut = getUntilOut(),
        stepOut = getStepOut(),
        indexVariableId = getIndexVariableId(),
    )
}

internal fun FloatFunctionCall.readData(): FloatFunctionCallData {
    return FloatFunctionCallData(
        function = getFunction(),
        outArgs = getOutArgs() as? FloatArray,
    )
}

internal fun Custom.readData(): CustomData {
    return CustomData(
        config = getConfig() as? String,
        configId = getConfigId(),
        properties = getProperties(),
    )
}

// 1. CoreDocument Helpers
@Suppress("UNCHECKED_CAST")
internal fun androidx.compose.remote.core.CoreDocument.getOperationsReflection():
    ArrayList<androidx.compose.remote.core.Operation> {
    return getOperations() as ArrayList<androidx.compose.remote.core.Operation>
}

@Suppress("UNCHECKED_CAST")
internal fun androidx.compose.remote.core.CoreDocument.getFloatExpressionsReflection():
    java.util.HashMap<Int, androidx.compose.remote.core.operations.FloatExpression> {
    return getFloatExpressions()
        as java.util.HashMap<Int, androidx.compose.remote.core.operations.FloatExpression>
}

internal fun androidx.compose.remote.core.CoreDocument.registerVariablesReflection(
    context: androidx.compose.remote.core.RemoteContext,
    operations: ArrayList<androidx.compose.remote.core.Operation>,
) {
    registerVariables(context, operations)
}

internal fun androidx.compose.remote.core.CoreDocument.applyOperationsReflection(
    context: androidx.compose.remote.core.RemoteContext,
    operations: ArrayList<androidx.compose.remote.core.Operation>,
) {
    applyOperations(context, operations)
}

// 2. RemoteComposeState Helpers
internal fun androidx.compose.remote.core.RemoteComposeState.getRemoteContextReflection():
    androidx.compose.remote.core.RemoteContext? {
    return getRemoteContext() as? androidx.compose.remote.core.RemoteContext
}

// 3. Layout Component / Manager Helpers
internal fun androidx.compose.remote.core.operations.layout.LayoutComponent
    .getDrawContentOperationsListReflection():
    java.util.ArrayList<androidx.compose.remote.core.Operation>? {
    return getDrawContentOperations()?.list
}

internal val androidx.compose.remote.core.operations.layout.managers.LayoutManager.horizontalPositioningReflection:
    Int
    get() =
        when (this) {
            is androidx.compose.remote.core.operations.layout.managers.RowLayout ->
                horizontalPositioning
            is androidx.compose.remote.core.operations.layout.managers.ColumnLayout ->
                horizontalPositioning
            is androidx.compose.remote.core.operations.layout.managers.FitBoxLayout ->
                horizontalPositioning
            is androidx.compose.remote.core.operations.layout.managers.BoxLayout ->
                horizontalPositioning
            else -> throw NoSuchFieldException("mHorizontalPositioning not found in $javaClass")
        }

internal val androidx.compose.remote.core.operations.layout.managers.LayoutManager.verticalPositioningReflection:
    Int
    get() =
        when (this) {
            is androidx.compose.remote.core.operations.layout.managers.RowLayout ->
                verticalPositioning
            is androidx.compose.remote.core.operations.layout.managers.ColumnLayout ->
                verticalPositioning
            is androidx.compose.remote.core.operations.layout.managers.FitBoxLayout ->
                verticalPositioning
            is androidx.compose.remote.core.operations.layout.managers.BoxLayout ->
                verticalPositioning
            else -> throw NoSuchFieldException("mVerticalPositioning not found in $javaClass")
        }

// 4. Modifier Operations Helpers
internal fun androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
    .getVisibilityIdReflection(): Int {
    return getVisibilityId()
}

internal val androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation.valueReflection:
    Float
    get() = rawValue

// Marquee

internal fun androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
    .readDataReflection(): MarqueeModifierOperationData {
    return MarqueeModifierOperationData(
        iterations = getIterations(),
        animationMode = getAnimationMode(),
        repeatDelayMillis = getRepeatDelayMillis(),
        initialDelayMillis = getInitialDelayMillis(),
        spacing = getSpacing(),
        velocity = getVelocity(),
    )
}

// GraphicsLayer AttributeValue

internal fun androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
    .getValuesReflection(): List<GraphicsLayerAttributeValueData> {
    return values.map { item ->
        GraphicsLayerAttributeValueData(name = item.name, id = item.id, value = item.getValue())
    }
}

// HostNamedActionOperation

internal fun HostNamedActionOperation.readData(): HostNamedActionOperationData {
    return HostNamedActionOperationData(
        textId = getTextId(),
        type = getType(),
        valueId = getValueId(),
    )
}

// 5. Draw Operations Helpers

// DrawBase2

internal fun androidx.compose.remote.core.operations.DrawBase2.readDataReflection(): DrawBase2Data {
    return DrawBase2Data(
        v1 = getV1(),
        v2 = getV2(),
        value1 = getValue1(),
        value2 = getValue2(),
    )
}

// DrawBase3

internal fun androidx.compose.remote.core.operations.DrawBase3.readDataReflection(): DrawBase3Data {
    return DrawBase3Data(
        v1 = getV1(),
        v2 = getV2(),
        v3 = getV3(),
        value1 = getValue1(),
        value2 = getValue2(),
        value3 = getValue3(),
    )
}

// DrawBase4

internal fun androidx.compose.remote.core.operations.DrawBase4.readDataReflection(): DrawBase4Data {
    return DrawBase4Data(
        x1 = getRawX1(),
        y1 = getRawY1(),
        x2 = getRawX2(),
        y2 = getRawY2(),
        x1Value = getX1Value(),
        y1Value = getY1Value(),
        x2Value = getX2Value(),
        y2Value = getY2Value(),
    )
}

// DrawBase6

internal fun androidx.compose.remote.core.operations.DrawBase6.readDataReflection(): DrawBase6Data {
    return DrawBase6Data(
        v1 = getV1(),
        v2 = getV2(),
        v3 = getV3(),
        v4 = getV4(),
        v5 = getV5(),
        v6 = getV6(),
        value1 = getValue1(),
        value2 = getValue2(),
        value3 = getValue3(),
        value4 = getValue4(),
        value5 = getValue5(),
        value6 = getValue6(),
    )
}

// DrawBitmap

internal fun androidx.compose.remote.core.operations.DrawBitmap.readDataReflection():
    DrawBitmapData {
    return DrawBitmapData(
        left = getLeft(),
        top = getTop(),
        right = getRight(),
        bottom = getBottom(),
        outputLeft = getOutputLeft(),
        outputTop = getOutputTop(),
        outputRight = getOutputRight(),
        outputBottom = getOutputBottom(),
        id = getImageId(),
        descriptionId = getDescriptionId(),
    )
}

// DrawBitmapScaled

internal fun androidx.compose.remote.core.operations.DrawBitmapScaled.readDataReflection():
    DrawBitmapScaledData {
    return DrawBitmapScaledData(
        imageId = getImageId(),
        srcLeft = getSrcLeft(),
        outSrcLeft = getOutSrcLeft(),
        srcTop = getSrcTop(),
        outSrcTop = getOutSrcTop(),
        srcRight = getSrcRight(),
        outSrcRight = getOutSrcRight(),
        srcBottom = getSrcBottom(),
        outSrcBottom = getOutSrcBottom(),
        dstLeft = getDstLeft(),
        outDstLeft = getOutDstLeft(),
        dstTop = getDstTop(),
        outDstTop = getOutDstTop(),
        dstRight = getDstRight(),
        outDstRight = getOutDstRight(),
        dstBottom = getDstBottom(),
        outDstBottom = getOutDstBottom(),
        contentDescId = getContentDescId(),
        scaleFactor = getScaleFactor(),
        outScaleFactor = getOutScaleFactor(),
        scaleType = getScaleType(),
    )
}

// DrawPath

internal fun androidx.compose.remote.core.operations.DrawPath.readDataReflection(): DrawPathData {
    return DrawPathData(
        id = getPathId(),
        start = getStart(),
        end = getEnd(),
    )
}

// BitmapData

internal fun androidx.compose.remote.core.operations.BitmapData.readDataReflection():
    BitmapDataData {
    return BitmapDataData(
        imageId = mImageId,
        imageWidth = getImageWidth(),
        imageHeight = getImageHeight(),
        type = getType().toShort(),
        encoding = getEncoding(),
        bitmap = getBitmap() as ByteArray,
    )
}

// PaintBundle
internal fun androidx.compose.remote.core.operations.paint.PaintBundle.getArrayReflection():
    IntArray {
    return getArray() as IntArray
}

internal fun androidx.compose.remote.core.operations.paint.PaintBundle.getPosReflection(): Int {
    return getPos()
}

// CoreSemantics nullable content description
internal fun androidx.compose.remote.core.semantics.CoreSemantics
    .getContentDescriptionIdReflection(): Int? {
    val id = mContentDescriptionId
    return if (id != 0) id else null
}

// Recollect collections
internal fun androidx.compose.remote.core.CoreDocument.recollectCollectionsReflection() {
    val operations = this.getOperationsReflection()
    val state = this.remoteComposeState
    collectCollections(operations, state)
}

internal fun androidx.compose.remote.core.CoreDocument.updateTimeReflection(
    context: androidx.compose.remote.core.RemoteContext
) {
    mTimeVariables.updateTime(context)
}

internal fun androidx.compose.remote.core.RemoteContext.getVariableIdReflection(name: String): Int {
    val context =
        this as? androidx.compose.remote.player.core.platform.AndroidRemoteContext ?: return -1
    return try {
        context.getVariableId(name)
    } catch (e: NoSuchElementException) {
        -1
    }
}

// --- Action Operations Helpers ---

// ValueIntegerChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation.targetValueIdReflection:
    Int
    get() = targetValueId
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation.valueReflection:
    Int
    get() = value

// ValueFloatChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation.targetValueIdReflection:
    Int
    get() = getTargetValueId()
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation.valueReflection:
    Float
    get() = getValue()

// ValueStringChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation.targetValueIdReflection:
    Int
    get() = getTargetValueId()
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation.valueIdReflection:
    Int
    get() = getValueId()

// ValueIntegerExpressionChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation.targetValueIdReflection:
    Long
    get() = targetValueId
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation.valueExpressionIdReflection:
    Long
    get() = valueExpressionId

// ValueFloatExpressionChangeActionOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation.targetValueIdReflection:
    Int
    get() = targetValueId
internal val androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation.valueExpressionIdReflection:
    Int
    get() = valueExpressionId

// --- BorderModifierOperation Helper ---

internal fun androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
    .readDataReflection(): BorderModifierOperationData {
    return BorderModifierOperationData(
        useColorId = getUseColorId(),
        colorId = getColorId(),
        r = getR(),
        g = getG(),
        b = getB(),
        a = getA(),
        borderWidth = getBorderWidth(),
        roundedCorner = getRoundedCorner(),
        shapeType = getShapeType(),
    )
}

// --- BackgroundModifierOperation Helper ---

internal fun androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
    .readDataReflection(): BackgroundModifierOperationData {
    return BackgroundModifierOperationData(
        useColorId = getUseColorId(),
        colorId = getColorId(),
        rId = getRId(),
        gId = getGId(),
        bId = getBId(),
        aId = getAId(),
        shapeType = getShapeType(),
    )
}

// --- StateLayout Helper ---
internal val androidx.compose.remote.core.operations.layout.managers.StateLayout.indexIdReflection:
    Int
    get() = getIndexId()

// --- CoreDocument updateVariables Helper ---
internal fun androidx.compose.remote.core.CoreDocument.updateVariablesReflection(
    context: androidx.compose.remote.core.RemoteContext,
    theme: Int,
    operations: List<androidx.compose.remote.core.Operation>,
) {
    updateVariables(context, theme, operations)
}

// AlignByModifierOperation
internal val androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation.lineReflection:
    Float
    get() = getLine()

// --- CoreText Reflection Helper ---

internal fun androidx.compose.remote.core.operations.layout.managers.CoreText.readDataReflection():
    CoreTextData {
    return CoreTextData(
        colorValue = getColorValue(),
        fontSizeValue = getFontSizeValue(),
        type = getType(),
        fontWeightValue = getFontWeightValue(),
        fontStyle = getFontStyle(),
        textAlignValue = getTextAlignValue(),
        overflow = getOverflow(),
        maxLines = getMaxLines(),
        letterSpacing = getLetterSpacing(),
        lineHeightMultiplier = getLineHeightMultiplier(),
        lineHeightAdd = getLineHeightAdd(),
    )
}

// --- TextLayout Reflection Helper ---

internal fun androidx.compose.remote.core.operations.layout.managers.TextLayout
    .readDataReflection(): TextLayoutData {
    return TextLayoutData(
        colorValue = getColorValue(),
        fontSizeValue = getFontSizeValue(),
        type = getType(),
        fontWeight = getFontWeight(),
        textAlignValue = getTextAlignValue(),
        overflow = getOverflow(),
        maxLines = getMaxLines(),
    )
}

// 6. Layout Spacing Helpers
internal fun rowSpacedBy(layout: RowLayout): Float = layout.spacedBy

internal fun columnSpacedBy(layout: ColumnLayout): Float = layout.spacedBy

// 7. ScrollModifier Reflection
internal fun scrollPosition(op: ScrollModifierOperation): Float = op.positionExpression

internal fun touchStopMode(touch: TouchExpression): Int = touch.stopMode

internal fun touchStopSpec(touch: TouchExpression): FloatArray? =
    touch.stopSpec

// 8. DimensionConstraints Reflection
internal fun dimensionConstraintsType(op: DimensionConstraintsModifierOperation): Int =
    op.type

// 9. DimensionModifier Reflection
internal fun dimensionRawValue(op: DimensionModifierOperation): Float = op.rawValue
