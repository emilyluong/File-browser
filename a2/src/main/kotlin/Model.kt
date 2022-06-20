import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.shape.Shape
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.IOException
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files


class Model (private val stage: Stage) {

    // all views of this model
    private val views: ArrayList<IView> = ArrayList()
    var initialCanvasState = ArrayList<Shape>()

    var selectedTool = "select"
    var selectedToolProperty = HashMap<String, String>()
    var shapesOnCanvas = ArrayList<Shape>()
    var currentSelectedShape: Shape? = null
    var canvas = Canvas()
    var gc = canvas.graphicsContext2D
    var fileToLoad: String? = null
    var isPrevToolSelect = false
    var editAction: String? = null
    val clipboard = Clipboard.getSystemClipboard()
    var pastedShape: Shape? = null
    var isToolChanged = false

    init {
        selectedToolProperty["lineColour"] = "#000000"
        selectedToolProperty["fillColour"] = "#FFFFFF"
        selectedToolProperty["lineThickness"] = "1.0"
        selectedToolProperty["lineStyle"] = "1.0"
    }


    // method that the views can use to register themselves with the Model
    // once added, they are told to update and get state from the Model
    fun addView(view: IView) {
        views.add(view)
        view.updateView()
    }

    // the model uses this method to notify all of the Views that the data has changed
    // the expectation is that the Views will refresh themselves to display new data when appropriate
    fun notifyObservers() {
        for (view in views) {
            view.updateView()
        }
    }

    fun isCanvasDirty(): Boolean {
        return shapesOnCanvas != initialCanvasState
    }

    fun promptToSave() {
        val saveBtn = ButtonType("Save", ButtonBar.ButtonData.OK_DONE)
        val noSaveBtn = ButtonType("Don't Save", ButtonBar.ButtonData.OTHER)

        val savePopup = Alert(Alert.AlertType.WARNING, "If you would like to save these changes, click Save.", noSaveBtn, saveBtn)
        savePopup.title = "Save Changes"
        savePopup.headerText = "You have unsaved changes!"
        val result = savePopup.showAndWait()

        if (result.get().text == "Save") {
            saveFile()
        }
    }

    fun newFile() {
        if (isCanvasDirty()) {
            promptToSave()
        }

        resetCanvas()
    }

    fun resetCanvas() {
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)

        // init to default settings
        selectedTool = "select"
        selectedToolProperty = HashMap()
        shapesOnCanvas = ArrayList()
        currentSelectedShape = null
        initialCanvasState = ArrayList()

        selectedToolProperty["lineColour"] = "#000000"
        selectedToolProperty["fillColour"] = "#FFFFFF"
        selectedToolProperty["lineThickness"] = "1.0"
        selectedToolProperty["lineStyle"] = "1.0"
    }

    fun loadFile() {
        if (isCanvasDirty()) {
            promptToSave()
        }

        val fileChooser = FileChooser()
        val file = fileChooser.showOpenDialog(stage)
        if (file != null) {
            fileToLoad = Files.readString(file.toPath(), StandardCharsets.US_ASCII)
            notifyObservers()
        }
    }

    fun addShapeDataToCanvas(jsonShapeData: String) {
        val shapeDataListObj = Json.decodeFromString<ShapeDataList>(jsonShapeData)
        val shapeDataList = shapeDataListObj.shapeDataList

        for (shapeData in shapeDataList) {

            when(shapeData.shapeType) {
                "line" -> {
                    addLine(Color.web(shapeData.lineColor), shapeData.thickness, shapeData.style, shapeData.startX, shapeData.startY, shapeData.endX, shapeData.endY)
                } "circle" -> {
                    addCircle(Color.web(shapeData.lineColor), Color.web(shapeData.fillColor), shapeData.thickness, shapeData.style, shapeData.centerX, shapeData.centerY, shapeData.radius)
                } "rectangle" -> {
                    addRectangle(Color.web(shapeData.lineColor), Color.web(shapeData.fillColor), shapeData.thickness, shapeData.style, shapeData.x, shapeData.y, shapeData.width, shapeData.height)
                }
            }
        }
    }

    fun getShapeData(shape: Shape): ShapeData? {
        if (shape is Line) {
            return ShapeData(
                "line",
                getColorHexString(shape.stroke.toString()),
                getColorHexString(shape.stroke.toString()),
                shape.strokeWidth,
                shape.strokeDashArray,
                shape.startX,
                shape.startY,
                shape.endX,
                shape.endY,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
            )
        } else if (shape is Circle) {
            return ShapeData(
                "circle",
                getColorHexString(shape.stroke.toString()),
                getColorHexString(shape.fill.toString()),
                shape.strokeWidth,
                shape.strokeDashArray,
                0.0,
                0.0,
                0.0,
                0.0,
                shape.centerX,
                shape.centerY,
                shape.radius,
                0.0,
                0.0,
                0.0,
                0.0
            )
        } else if (shape is Rectangle) {
            return ShapeData(
                "rectangle",
                getColorHexString(shape.stroke.toString()),
                getColorHexString(shape.fill.toString()),
                shape.strokeWidth,
                shape.strokeDashArray,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                shape.x,
                shape.y,
                shape.width,
                shape.height
            )
        }
        return null
    }

    fun saveFile() {
        val fileChooser = FileChooser()
        val file = fileChooser.showSaveDialog(stage)

        if (file != null) {

            val shapeList = ArrayList<ShapeData>()
            for (shape in shapesOnCanvas) {
                val shapeData = getShapeData(shape)
                if (shapeData != null) {
                    shapeList.add(shapeData)
                }
            }

            val jsonShapeList = ShapeDataList(shapeList)

            try {
                val jsonString = Json.encodeToString(jsonShapeList)
                PrintWriter(FileWriter(file)).use { it.write(jsonString) }
                initialCanvasState = shapesOnCanvas.clone() as ArrayList<Shape>
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun exit() {
        if (isCanvasDirty()) {
            promptToSave()
        }
        Platform.exit()
    }


    fun cutShape() {
        copyShape()
        editAction = "cut"
        notifyObservers()
    }

    fun copyShape() {
        val content = ClipboardContent()

        val shapeData = getShapeData(currentSelectedShape!!)

        var jsonString = ""
        try {
            jsonString = Json.encodeToString(shapeData)
        } catch(e: IOException) {
            e.printStackTrace()
        }

        content.putString(jsonString)
        clipboard.setContent(content)
    }

    fun pasteShape() {
        var content = ""
        if (clipboard.hasContent(DataFormat.PLAIN_TEXT)) {
            content = clipboard.string
        } else {
            return
        }

        try {
            val shapeData = Json.decodeFromString<ShapeData>(content)

            when(shapeData.shapeType) {
                "line" -> {
                    pastedShape = if (pastedShape != null && pastedShape is Line) {
                        addLine(Color.web(shapeData.lineColor), shapeData.thickness, shapeData.style, (pastedShape as Line).startX + 20, (pastedShape as Line).startY + 20, (pastedShape as Line).endX + 20, (pastedShape as Line).endY + 20)
                    } else {
                        addLine(Color.web(shapeData.lineColor), shapeData.thickness, shapeData.style, shapeData.startX + 20, shapeData.startY + 20, shapeData.endX + 20, shapeData.endY + 20)
                    }
                } "circle" -> {
                    pastedShape = if (pastedShape != null && pastedShape is Circle) {
                        addCircle(Color.web(shapeData.lineColor), Color.web(shapeData.fillColor), shapeData.thickness, shapeData.style, (pastedShape as Circle).centerX + 20, (pastedShape as Circle).centerY + 20, shapeData.radius)
                    } else {
                        addCircle(Color.web(shapeData.lineColor), Color.web(shapeData.fillColor), shapeData.thickness, shapeData.style, shapeData.centerX + 20, shapeData.centerY + 20, shapeData.radius)
                    }
                } "rectangle" -> {
                    pastedShape = if (pastedShape != null && pastedShape is Rectangle) {
                        addRectangle(Color.web(shapeData.lineColor), Color.web(shapeData.fillColor), shapeData.thickness, shapeData.style, (pastedShape as Rectangle).x + 20, (pastedShape as Rectangle).y + 20, shapeData.width, shapeData.height)
                    } else {
                        addRectangle(Color.web(shapeData.lineColor), Color.web(shapeData.fillColor), shapeData.thickness, shapeData.style, shapeData.x + 20, shapeData.y + 20, shapeData.width, shapeData.height)
                    }
                }
            }

            editAction = "paste"
            notifyObservers()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // changes state of selected tool
    fun changeTool(tool: String) {
        if (selectedTool != tool) {
            isToolChanged = true
            if (selectedTool == "select" && currentSelectedShape != null) {
                isPrevToolSelect = true
                // for dehighlighting after choosing another tool
                selectedTool = tool
                notifyObservers()
                currentSelectedShape = null
            } else {
                selectedTool = tool
                // for disabling/enabling tool property option
                notifyObservers()
            }
            isToolChanged = false
        }
    }

    // changes state of selected tool property
    fun changeToolProperty(property: String, selected: String) {
        if (selectedToolProperty[property] != selected) {
            selectedToolProperty[property] = selected
            notifyObservers()
        }
    }

    fun updateShapesOnCanvas(currentShapes: ArrayList<Shape>) {
        shapesOnCanvas = currentShapes
    }

    // add shapes to global variable to keep track of canvas when canvas gets deleted from preview drawing
    fun addShape(shape: Shape) {
        shapesOnCanvas.add(shape)
    }

    fun eraseShape(shape: Shape, isCompareEffect: Boolean) {
        removeShapeFromCanvas(shape, isCompareEffect)
        if (currentSelectedShape == shape) {
            currentSelectedShape = null
        }
    }

    fun isShapesEqual(shapeA: Shape, shapeB: Shape, isCompareEffect: Boolean): Boolean {
        fun getLineCond(shape: Line, shapeToDelete: Line): Boolean {
            return shapeToDelete.stroke == shape.stroke
                    && shapeToDelete.strokeWidth == shape.strokeWidth && shapeToDelete.strokeDashArray == shape.strokeDashArray
                    && shapeToDelete.startX == shape.startX && shapeToDelete.startY == shape.startY
                    && shapeToDelete.endX == shape.endX && shapeToDelete.endY == shape.endY
        }

        fun getCircleCond(shape: Circle, shapeToDelete: Circle): Boolean {
            return shapeToDelete.stroke == shape.stroke && shapeToDelete.fill == shape.fill
                    && shapeToDelete.strokeWidth == shape.strokeWidth && shapeToDelete.strokeDashArray == shape.strokeDashArray
                    && shapeToDelete.centerX == shape.centerX && shapeToDelete.centerY == shape.centerY
                    && shapeToDelete.radius == shape.radius
        }

        fun getRectangleCond(shape: Rectangle, shapeToDelete: Rectangle): Boolean {
            return shapeToDelete.stroke == shape.stroke && shapeToDelete.fill == shape.fill
                    && shapeToDelete.strokeWidth == shape.strokeWidth && shapeToDelete.strokeDashArray == shape.strokeDashArray
                    && shapeToDelete.x == shape.x && shapeToDelete.y == shape.y
                    && shapeToDelete.width == shape.width && shapeToDelete.height == shape.height
        }

        if (shapeA is Line && shapeB is Line) {
            if (isCompareEffect) {
                return shapeB.effect == shapeA.effect && getLineCond(shapeA, shapeB)
            } else {
                return getLineCond(shapeA, shapeB)
            }
        } else if (shapeA is Circle && shapeB is Circle) {
            if (isCompareEffect) {
                return shapeB.effect == shapeA.effect && getCircleCond(shapeA, shapeB)
            } else {
                return getCircleCond(shapeA, shapeB)
            }
        } else if (shapeA is Rectangle && shapeB is Rectangle) {
            if (isCompareEffect) {
                return shapeB.effect == shapeA.effect && getRectangleCond(shapeA, shapeB)
            } else {
                return getRectangleCond(shapeA, shapeB)
            }
        }
        return false
    }

    fun removeShapeFromCanvas(shapeToDelete: Shape, isCompareEffect: Boolean) {

        val updatedShapes = ArrayList<Shape>()
        val currentShapes = shapesOnCanvas

        for (i in currentShapes.indices) {
            val shape = currentShapes[i]
            if (isShapesEqual(shape, shapeToDelete, isCompareEffect)) {
                continue
            }
            updatedShapes.add(shape)
        }
        updateShapesOnCanvas(updatedShapes)
    }

    fun getLineColour(): Paint {
        return Color.web(selectedToolProperty["lineColour"])
    }

    fun getFillColour(): Paint {
        return Color.web(selectedToolProperty["fillColour"])
    }

    fun getLineThickness(): Double {
        return selectedToolProperty["lineThickness"]?.toDouble() ?: 1.0
    }

    fun getLineStyle(): ArrayList<Double> {
        val dashArray = selectedToolProperty["lineStyle"]?.split(",")

        val result = ArrayList<Double>()
        // convert to double elements
        if (dashArray != null) {
            for (dash in dashArray) {
                result.add(dash.toDouble())
            }
        }

        return result
    }

    fun getDistanceBetween(startX: Double, startY: Double, endX: Double, endY: Double): Double {
        return hypot(endX-startX, endY-startY)
    }

    fun getColorHexString(linePickerStr: String): String {
        val hexNum = linePickerStr.substring(2, 8)
        return "#$hexNum"
    }

    fun setCurrentToolPropertyOfShape(shape: Shape) {
        val lineColour = getColorHexString(shape.stroke.toString())
        var fillColour = "#FFFFFF"
        if (shape is Circle || shape is Rectangle) {
            fillColour = getColorHexString(shape.fill.toString())
        }
        val lineThickness = shape.strokeWidth.toString()
        val lineStyle = shape.strokeDashArray

        var lineStyleString = "1.0"
        if (lineStyle.size == 1) {
            lineStyleString = "${lineStyle[0]}"
        } else if (lineStyle.size == 2) {
            lineStyleString = "${lineStyle[0]},${lineStyle[1]}"
        }

        selectedToolProperty["lineColour"] = lineColour
        selectedToolProperty["fillColour"] = fillColour
        selectedToolProperty["lineThickness"] = lineThickness
        selectedToolProperty["lineStyle"] = lineStyleString
    }

    fun getSelectedShape(x: Double, y: Double): Shape? {
        selectShapeOnPos(x, y)
        notifyObservers()
        return currentSelectedShape
    }

    fun selectShapeOnPos(x: Double, y: Double) {
        // gp though the shape array on the canvas and
        for (i in shapesOnCanvas.size-1 downTo 0) {
            val shape = shapesOnCanvas[i]

            if (shape is Line) {
                // if x y coordinates in where a line is
                val startLineX = shape.startX
                val startLineY = shape.startY
                val endLineX = shape.endX
                val endLineY = shape.endY

                val lineDistance = getDistanceBetween(startLineX, startLineY, endLineX, endLineY)
                val startLineAndCurrPosDistance = getDistanceBetween(startLineX, startLineY, x, y)
                val currPosAndEndLineDistance = getDistanceBetween(x, y, endLineX, endLineY)

                // check the estimated line distance against the actual line distance within +-1
                val estimatedLineDistance = startLineAndCurrPosDistance + currPosAndEndLineDistance
                if (estimatedLineDistance <= lineDistance + 1 && estimatedLineDistance >= lineDistance - 1) {
                    currentSelectedShape = shape
                    if (selectedTool == "select") {
                        setCurrentToolPropertyOfShape(shape)
                    }
                    return
                }
            } else if (shape is Circle) {
                val centerX = shape.centerX
                val centerY = shape.centerY
                val radius = shape.radius

                val estimatedRadius = sqrt((x - centerX).pow(2.0) + (y - centerY).pow(2.0))
                if (estimatedRadius <= radius) {
                    currentSelectedShape = shape
                    if (selectedTool == "select") {
                        setCurrentToolPropertyOfShape(shape)
                    }
                    return
                }
            } else if (shape is Rectangle) {
                val topLeftX = shape.x
                val topLeftY = shape.y
                val width = shape.width
                val height = shape.height

                if((x >= topLeftX) && (x <= topLeftX + width) &&
                    (y >= topLeftY) && (y <= topLeftY + height)) {
                    currentSelectedShape = shape
                    if (selectedTool == "select") {
                        setCurrentToolPropertyOfShape(shape)
                    }
                    return
                }
            }
        }
        currentSelectedShape = null
        return
    }

    fun addLine(lineColor: Paint, lineThickness: Double, lineStyle: List<Double>, startX: Double, startY: Double, endX: Double, endY: Double): Line {
        val line = Line(startX, startY, endX, endY)
        line.stroke = lineColor
        line.strokeWidth = lineThickness
        line.strokeDashArray.addAll(lineStyle)
        addShape(line)
        return line
    }

    fun addCircle(lineColor: Paint, fillColor: Paint, lineThickness: Double, lineStyle: List<Double>, centerX: Double, centerY: Double, radius: Double): Circle {
        val circle = Circle(centerX, centerY, radius)
        circle.stroke = lineColor
        circle.fill = fillColor
        circle.strokeWidth = lineThickness
        circle.strokeDashArray.addAll(lineStyle)
        addShape(circle)
        return circle
    }

    fun addRectangle(lineColor: Paint, fillColor: Paint, lineThickness: Double, lineStyle: List<Double>, startX: Double, startY: Double, width: Double, height: Double): Rectangle {
        val rectangle = Rectangle(startX, startY, width, height)
        rectangle.stroke = lineColor
        rectangle.fill = fillColor
        rectangle.strokeWidth = lineThickness
        rectangle.strokeDashArray.addAll(lineStyle)
        addShape(rectangle)
        return rectangle
    }
}