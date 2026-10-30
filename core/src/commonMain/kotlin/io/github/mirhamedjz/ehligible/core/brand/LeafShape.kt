package io.github.mirhamedjz.ehligible.core.brand

/**
 * The Eh-ligible leaf: a softened 11-point maple leaf in a 100×100 box, drawn with rounded joins and tilted.
 * Shared by the app, the Android widget and the iOS widget so they all draw the same mark.
 */
object LeafShape {
    /** Outline points (x, y), clockwise from the top tip. */
    val points: List<Pair<Float, Float>> = listOf(
        50f to 4f, 57f to 18f, 65f to 14f, 62f to 36f, 80f to 22f, 84f to 32f, 96f to 30f, 88f to 48f,
        94f to 54f, 70f to 70f, 73f to 80f, 53f to 77f, 53f to 97f, 47f to 97f, 47f to 77f, 27f to 80f,
        30f to 70f, 6f to 54f, 12f to 48f, 4f to 30f, 16f to 32f, 20f to 22f, 38f to 36f, 35f to 14f, 43f to 18f,
    )

    const val TILT_DEGREES: Float = -12f

    /** Width of the rounding stroke, as a fraction of the drawing's side. */
    const val JOIN_FRACTION: Float = 0.03f
}
