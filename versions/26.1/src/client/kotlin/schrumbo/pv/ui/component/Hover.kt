package schrumbo.pv.ui.component

/**
 * The real, unscaled screen mouse position. Hit-testing uses the (possibly scaled) local mouse
 * passed into `render`, but tooltips must be placed at the true cursor, so they read this instead.
 */
object Hover {
    var screenX = 0
    var screenY = 0
}
