import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.shape.Shape
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import netscape.javascript.JSObject
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files


class Model (private val stage: Stage) {

    // all views of this model
    private val views: ArrayList<IView> = ArrayList()

    var selectedTool = "select"
    var selectedToolProperty = HashMap<String, String>()
    var shapesOnCanvas = ArrayList<Shape>()
    var currentSelectedShape: Shape? = null
    var canvas = Canvas()
    var gc = canvas.graphicsContext2D
    var isCanvasSaved = false
    var fileToLoad: String? = null
    var isPrevToolSelect = false

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
        return shapesOnCanvas.size != 0
    }

    fun promptToSave() {
        if (isCanvasDirty()) {
            val saveBtn = ButtonType("Save", ButtonBar.ButtonData.OK_DONE)
            val noSaveBtn = ButtonType("Don't Save", ButtonBar.ButtonData.OTHER)

            val savePopup = Alert(Alert.AlertType.WARNING, "If you would like to save these changes, click Save.", noSaveBtn, saveBtn)
            savePopup.title = "Save Changes"
            savePopup.headerText = "You have unsaved changes!"
            val result = savePopup.showAndWait()

            println("result " + result)

            if (result.get().text == "Save") {
                saveFile()
                isCanvasSaved = true
            } else if (result.get().text == "Don't Save") {
                isCanvasSaved = false
            }
        }
    }

    fun newFile() {
        if (!isCanvasSaved) {
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

        selectedToolProperty["lineColour"] = "#000000"
        selectedToolProperty["fillColour"] = "#FFFFFF"
        selectedToolProperty["lineThickness"] = "1.0"
        selectedToolProperty["lineStyle"] = "1.0"
    }

    fun loadFile() {
        if (!isCanvasSaved) {
            promptToSave()
        }

        val fileChooser = FileChooser()
        val file = fileChooser.showOpenDialog(stage)
        println("file " + file + " " + file.extension)
        if (file != null) {
            fileToLoad = Files.readString(file.toPath(), StandardCharsets.US_ASCII)
            println("file to load " + fileToLoad)
            notifyObservers()
        }
    }

    fun saveFile() {
        val fileChooser = FileChooser()
        val file = fileChooser.showSaveDialog(stage)

        if (file != null) {

            val shapeList = ArrayList<ShapeData>()
            for (shape in shapesOnCanvas) {
                if (shape is Line) {
                    shapeList.add(ShapeData(
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
                    ))
                } else if (shape is Circle) {
                    shapeList.add(ShapeData(
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
                    ))
                } else if (shape is Rectangle) {
                    shapeList.add(ShapeData(
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
                    ))
                }
            }

            val jsonShapeList = ShapeDataList(shapeList)
            val jsonString = Json.encodeToString(jsonShapeList)
            try {
                PrintWriter(FileWriter(file)).use { it.write(jsonString) }
                isCanvasSaved = true
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    fun exit() {
        if (!isCanvasSaved) {
            promptToSave()
        }
        Platform.exit()
    }

    // changes state of selected tool
    fun changeTool(tool: String) {
        println("change tool " + tool)
        //println("selected tool property change tool " + selectedToolProperty)
        if (selectedTool != tool) {
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
        }
    }

    // changes state of selected tool property
    fun changeToolProperty(property: String, selected: String) {
        if (selectedToolProperty[property] != selected) {
            selectedToolProperty[property] = selected
            notifyObservers()
            println("selected tool property " + selectedToolProperty)

        }
    }

    fun updateShapesOnCanvas(currentShapes: ArrayList<Shape>) {
        shapesOnCanvas = currentShapes
    }

    // add shapes to global variable to keep track of canvas when canvas gets deleted from preview drawing
    fun addShape(shape: Shape) {
        //println("add shape effect " + shape.effect)
        shapesOnCanvas.add(shape)
        //println("camvaseafteraddomg shape " + shapesOnCanvas)
    }

    fun eraseShape(shape: Shape) {
        //println("hehrheghireng " + (shapesOnCanvas.get(0) == currentSelectedShape))
        removeShapeFromCanvas(shape)
        if (currentSelectedShape == shape) {
            currentSelectedShape = null
        }
    }

    fun removeShapeFromCanvas(shapeToDelete: Shape) {
        val updatedShapes = ArrayList<Shape>()
        val currentShapes = shapesOnCanvas

        for (i in currentShapes.indices) {
            val shape = currentShapes[i]

            if (shape is Line && shapeToDelete is Line) {
                if (shapeToDelete.effect == shape.effect && shapeToDelete.stroke == shape.stroke
                    && shapeToDelete.strokeWidth == shape.strokeWidth && shapeToDelete.strokeDashArray == shape.strokeDashArray
                    && shapeToDelete.startX == shape.startX && shapeToDelete.startY == shape.startY
                    && shapeToDelete.endX == shape.endX && shapeToDelete.endY == shape.endY) {
                    //println("line match")
                    continue
                }
            } else if (shape is Circle && shapeToDelete is Circle) {
                if (shapeToDelete.effect == shape.effect && shapeToDelete.stroke == shape.stroke && shapeToDelete.fill == shape.fill
                    && shapeToDelete.strokeWidth == shape.strokeWidth && shapeToDelete.strokeDashArray == shape.strokeDashArray
                    && shapeToDelete.centerX == shape.centerX && shapeToDelete.centerY == shape.centerY
                    && shapeToDelete.radius == shape.radius) {
                    //println("circle match")
                    continue
                }
            } else if (shape is Rectangle && shapeToDelete is Rectangle) {
                if (shapeToDelete.effect == shape.effect && shapeToDelete.stroke == shape.stroke && shapeToDelete.fill == shape.fill
                    && shapeToDelete.strokeWidth == shape.strokeWidth && shapeToDelete.strokeDashArray == shape.strokeDashArray
                    && shapeToDelete.x == shape.x && shapeToDelete.y == shape.y
                    && shapeToDelete.width == shape.width && shapeToDelete.height == shape.height) {
                    //println("rect match")
                    continue
                }
            }

            updatedShapes.add(currentShapes[i])
        }
        updateShapesOnCanvas(updatedShapes)
    }

    fun getLineColour(): Paint {
        return Color.web(selectedToolProperty["lineColour"])
    }

    fun getFillColour(): Paint {
        //println("getfillcolour " + selectedToolProperty["fillColour"])
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
        //println("hex num #$hexNum")
        return "#$hexNum"
    }

    fun setCurrentToolPropertyOfShape(shape: Shape) {
        val lineColour = getColorHexString(shape.stroke.toString())
        val fillColour = getColorHexString(shape.fill.toString())
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

        //println("selecttoolprop " + selectedToolProperty)
    }

    fun getSelectedShape(x: Double, y: Double): Shape? {
        selectShapeOnPos(x, y)
        notifyObservers()
        return currentSelectedShape
    }

    fun selectShapeOnPos(x: Double, y: Double) {
        // gp though the shape array on the canvas and
        //println("shape array " + shapesOnCanvas )
        //println("x " + x + " y " + y)

        for (shape in shapesOnCanvas) {

            if (shape is Line) {
                // if x y coordinates in where a line is
                val startLineX = shape.startX
                val startLineY = shape.startY
                val endLineX = shape.endX
                val endLineY = shape.endY

                // can check if the shape is selected by comparing the length of the line
                // to the sum of the distance of the start line coordinates to the current coordinates
                // and the distance of the current coordinates to the end of the line coordinates
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
        //println("release " + centerX + " " + centerY + " " + radius)
        val circle = Circle(centerX, centerY, radius)
        circle.stroke = lineColor
        circle.fill = fillColor
        circle.strokeWidth = lineThickness
        circle.strokeDashArray.addAll(lineStyle)
        addShape(circle)
        println("cenx " + centerX + " cemy " + centerY + " rad " + radius)
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