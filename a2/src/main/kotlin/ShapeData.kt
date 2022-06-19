import kotlinx.serialization.Serializable

@Serializable
class ShapeData (
    val shapeType: String,
    val lineColor: String,
    //fill colour for circ/rect
    val fillColor: String,
    val thickness: Double,
    val style: List<Double>,
    // for line
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
    // for circle
    val centerX: Double,
    val centerY: Double,
    val radius: Double,
    // for rectangle
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)