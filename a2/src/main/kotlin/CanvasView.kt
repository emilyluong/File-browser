
import javafx.scene.canvas.Canvas
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.shape.Shape
import kotlin.math.pow
import kotlin.math.sqrt


internal class CanvasView (private val model: Model) : VBox(), IView {

    private var canvas = Canvas()
    val gc = canvas.graphicsContext2D

    private enum class STATE { NONE, DRAG }
    private var state = STATE.NONE

    //captures the current x, y coordinates of the mouse on the canvas
    private var prevX = 0.0
    private var prevY = 0.0

    //captures the start x, y coordinates of the mouse on the canvas
    private var startX = 0.0
    private var startY = 0.0


    // When notified by the model that things have changed,
    // update to display the new value
    override fun updateView() {
        if (model.editAction == "paste" || model.editAction == "cut") {
            // if these action r performed, want to remove the highlight of a selected shape first
            if (model.currentSelectedShape != null) {
                removeSelectedHighlightAndRefresh(model.currentSelectedShape!!)

                if (model.editAction == "cut") {
                    model.eraseShape(model.currentSelectedShape!!, false)
                }

                model.currentSelectedShape = null
            }
            gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
            refreshCanvas()
            model.editAction = null
        }

        // If there is a file to load
        val jsonShapeData = model.fileToLoad
        if (jsonShapeData != null) {
            val shape = model.currentSelectedShape
            if (shape != null) {
                removeSelectedHighlightAndRefresh(shape)
                model.currentSelectedShape = null
            }
            model.resetCanvas()
            model.addShapeDataToCanvas(jsonShapeData)

            // keep inital state to know when to prompt user
            model.initialCanvasState = model.shapesOnCanvas.clone() as ArrayList<Shape>

            refreshCanvas()

            // reset
            model.fileToLoad = null
        }

        // removes hightlight if choose another tool after "select"
        if (model.isPrevToolSelect) {
            removeSelectedHighlightAndRefresh(model.currentSelectedShape!!)
            model.isPrevToolSelect = false
        }

        // change the color of the selected shape
        if (model.selectedTool == "select" && model.currentSelectedShape != null) {

            when (val shape = model.currentSelectedShape) {
                is Line -> {
                    val line = model.addLine(model.getLineColour(), model.getLineThickness(), model.getLineStyle(), shape.startX, shape.startY, shape.endX, shape.endY)
                    model.removeShapeFromCanvas(shape, true)
                    model.currentSelectedShape = line
                    gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                    refreshCanvas()
                }
                is Circle -> {
                    val circle = model.addCircle(model.getLineColour(), model.getFillColour(), model.getLineThickness(), model.getLineStyle(), shape.centerX, shape.centerY, shape.radius)
                    model.removeShapeFromCanvas(shape, true)
                    model.currentSelectedShape = circle
                    gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                    refreshCanvas()
                }
                is Rectangle -> {
                    val rectangle = model.addRectangle(model.getLineColour(), model.getFillColour(), model.getLineThickness(), model.getLineStyle(), shape.x, shape.y, shape.width, shape.height)
                    model.removeShapeFromCanvas(shape, true)
                    model.currentSelectedShape = rectangle
                    gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                    refreshCanvas()
                }
            }
        }
    }

    // used to set the dash array of gc
    private fun setLineDashes(dashArray: List<Double>) {
        if (dashArray.size == 1) {
            gc.setLineDashes(dashArray[0])
        } else if (dashArray.size == 2) {
            gc.setLineDashes(dashArray[0], dashArray[1])
        }

    }

    private fun drawLine(selectedEffect: Effect?, lineColor: Paint, lineThickness: Double, lineStyle: List<Double>, startX: Double, startY: Double, endX: Double, endY: Double) {
        gc.setEffect(selectedEffect)
        gc.stroke = lineColor
        gc.lineWidth = lineThickness
        setLineDashes(lineStyle)
        gc.strokeLine(startX, startY, endX, endY)
    }

    private fun drawCircle(selectedEffect: Effect?, lineColor: Paint, fillColor: Paint, lineThickness: Double, lineStyle: List<Double>, startX: Double, startY: Double, radius: Double) {
        gc.setEffect(selectedEffect)
        gc.stroke = lineColor
        gc.fill = fillColor
        gc.lineWidth = lineThickness
        setLineDashes(lineStyle)
        gc.fillOval(startX, startY, radius, radius)
        gc.strokeOval(startX, startY, radius, radius)
    }

    private fun drawRectangle(selectedEffect: Effect?, lineColor: Paint, fillColor: Paint, lineThickness: Double, lineStyle: List<Double>, startX: Double, startY: Double, width: Double, height: Double) {
        gc.setEffect(selectedEffect)
        gc.stroke = lineColor
        gc.fill = fillColor
        gc.lineWidth = lineThickness
        setLineDashes(lineStyle)
        gc.fillRect(startX, startY, width, height)
        gc.strokeRect(startX, startY, width, height)
    }

    fun removeSelectedHighlightAndRefresh(shape: Shape) {
        model.removeShapeFromCanvas(shape, true)

        when(shape) {
            is Line -> {
                model.addLine(shape.stroke, shape.strokeWidth, shape.strokeDashArray, shape.startX, shape.startY, shape.endX, shape.endY)
            } is Circle -> {
                model.addCircle(shape.stroke, shape.fill, shape.strokeWidth, shape.strokeDashArray, shape.centerX, shape.centerY, shape.radius)
            } is Rectangle -> {
                model.addRectangle(shape.stroke, shape.fill, shape.strokeWidth, shape.strokeDashArray, shape.x, shape.y, shape.width, shape.height)
            }
        }

        gc.setEffect(null)
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
        refreshCanvas()
    }

    fun showSelectedHighlight(shape: Shape) {
        model.removeShapeFromCanvas(shape, true)
        shape.effect = DropShadow(shape.strokeWidth + 4.0, Color.BLUE)
        model.shapesOnCanvas.add(shape)
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
        refreshCanvas()
    }

    // updates the canvas when a new shape is added
    fun refreshCanvas() {
        val currentShapes = model.shapesOnCanvas

        for (shape in currentShapes) {
            val selectedEffect = shape.effect
            val lineColor = shape.stroke
            val fillColor = shape.fill
            val lineThickness = shape.strokeWidth
            val lineStyle = shape.strokeDashArray

            when (shape) {
                is Line -> {
                    drawLine(selectedEffect, lineColor, lineThickness, lineStyle, shape.startX, shape.startY, shape.endX, shape.endY)
                }
                is Circle -> {
                    drawCircle(selectedEffect, lineColor, fillColor, lineThickness, lineStyle, shape.centerX-shape.radius, shape.centerY-shape.radius, shape.radius*2)
                }
                is Rectangle -> {
                    drawRectangle(selectedEffect, lineColor, fillColor, lineThickness, lineStyle, shape.x, shape.y, shape.width, shape.height)
                }
            }
        }
    }

    // handles the resizing of the canvas
    fun resizeCanvas(width: Double, height: Double) {
        canvas.width = width
        canvas.height = height

        model.selectedTool = "select"
        val shape = model.currentSelectedShape
        if (shape != null) {
            removeSelectedHighlightAndRefresh(shape)
            model.currentSelectedShape = null
            model.notifyObservers()
        }
        refreshCanvas()

        // handles when the mouse is clicked for select, erase tool
        canvas.setOnMouseClicked {

            if (model.selectedTool == "select") {
                val prevSelectedShape = model.currentSelectedShape
                val selectedShape = model.getSelectedShape(it.x, it.y)

                if (selectedShape == null && prevSelectedShape != null) {
                    // if selected blank space
                    removeSelectedHighlightAndRefresh(prevSelectedShape)
                    model.currentSelectedShape = null
                } else if (prevSelectedShape != selectedShape && (selectedShape is Line || selectedShape is Circle || selectedShape is Rectangle)) {
                    // if there was a shape already selected before selecting this one
                    // remove the highlight first
                    if (prevSelectedShape != null) {
                        removeSelectedHighlightAndRefresh(prevSelectedShape)
                    }
                    showSelectedHighlight(selectedShape)
                }

            } else if (model.selectedTool == "erase") {
                val selectedShape = model.getSelectedShape(it.x, it.y)

                if (selectedShape != null) {
                    model.eraseShape(selectedShape, true)
                }
                gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                refreshCanvas()
                model.currentSelectedShape = null
            } else if (model.selectedTool == "fill") {
                val selectedShape = model.getSelectedShape(it.x, it.y)

                if (selectedShape != null) {
                    model.removeShapeFromCanvas(selectedShape, true)

                    selectedShape.fill = model.getFillColour()
                    model.currentSelectedShape = selectedShape

                    model.addShape(selectedShape)
                    gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                    refreshCanvas()
                    model.currentSelectedShape = null
                }
            }
        }

        // handles when mouse is released when line tool is used
        canvas.setOnMouseReleased {
            // add shape to list of shapes in the model
            if (state == STATE.DRAG) {
                when (model.selectedTool) {
                    "line" -> {
                        model.addLine(
                            model.getLineColour(),
                            model.getLineThickness(),
                            model.getLineStyle(),
                            startX,
                            startY,
                            prevX,
                            prevY
                        )
                        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                        refreshCanvas()
                    }
                    "circle" -> {
                        // check ensures that it doesnt add neg radius to array (when mouse is dragged other than top left to bottom right)
                        val radius = sqrt((prevX - startX).pow(2.0) + (prevY - startY).pow(2.0))
                        val centerX = startX
                        val centerY = startY

                        model.addCircle(
                            model.getLineColour(),
                            model.getFillColour(),
                            model.getLineThickness(),
                            model.getLineStyle(),
                            centerX,
                            centerY,
                            radius
                        )
                        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                        refreshCanvas()
                    }
                    "rectangle" -> {
                        // check ensures that it doesnt add neg radius to array (when mouse is dragged other than top left to bottom right)
                        if ((prevX - startX) > 0 && (prevY - startY) > 0) {
                            val width = prevX - startX
                            val height = prevY - startY
                            model.addRectangle(
                                model.getLineColour(),
                                model.getFillColour(),
                                model.getLineThickness(),
                                model.getLineStyle(),
                                startX,
                                startY,
                                width,
                                height
                            )
                            gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                            refreshCanvas()
                        }
                    }
                    "select" -> {
                        if (model.currentSelectedShape != null) {
                            state = STATE.NONE
                        }
                    }
                }
                state = STATE.NONE
            }
        }

        // get the start coordinates on the canvas when the mouse is pressed
        canvas.setOnMousePressed {
            startX = it.x
            startY = it.y
        }

        canvas.setOnMouseDragged {
            val lineColour = model.getLineColour()
            val fillColor = model.getFillColour()
            val lineThickness = model.getLineThickness()
            val lineStyle = model.getLineStyle()

            if (model.selectedTool == "line" || model.selectedTool == "circle" || model.selectedTool == "rectangle") {

                // clears the canvas whenever moving cursor to draw
                if (it.x != prevX || it.y != prevY) {
                    gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                }
                // update current x and y
                prevX = it.x
                prevY = it.y

                // selected tool will be drawn and then
                // update the canvas to include previously drawn shapes as they were deleted when moving cursor
                when (model.selectedTool) {
                    "line" -> {
                        refreshCanvas()
                        drawLine(null, lineColour, lineThickness, lineStyle, startX, startY, prevX, prevY)
                        state = STATE.DRAG
                    }
                    "circle" -> {
                        val radius = sqrt((prevX - startX).pow(2.0) + (prevY - startY).pow(2.0))

                        refreshCanvas()
                        drawCircle(null, lineColour, fillColor, lineThickness, lineStyle, startX - radius, startY - radius, radius * 2)
                        state = STATE.DRAG
                    }
                    "rectangle" -> {
                        val width = prevX-startX
                        val height = prevY-startY
                        refreshCanvas()
                        drawRectangle(null, lineColour, fillColor, lineThickness, lineStyle, startX, startY, width, height)
                        state = STATE.DRAG
                    }
                }
            } else if (model.selectedTool == "select") {
                val selectedShapeMoving = model.currentSelectedShape

                //delete selected shape from list of selected shape
                if (selectedShapeMoving != null) {
                    model.removeShapeFromCanvas(selectedShapeMoving, false)


                    prevX = it.x - startX
                    prevY = it.y - startY

                    startX = it.x
                    startY = it.y

                    when (selectedShapeMoving) {
                        is Line -> {
                            selectedShapeMoving.startX += prevX
                            selectedShapeMoving.startY += prevY
                            selectedShapeMoving.endX += prevX
                            selectedShapeMoving.endY += prevY
                        }
                        is Circle -> {
                            selectedShapeMoving.centerX += prevX
                            selectedShapeMoving.centerY += prevY
                        }
                        is Rectangle -> {
                            selectedShapeMoving.x += prevX
                            selectedShapeMoving.y += prevY
                        }
                    }

                    model.addShape(selectedShapeMoving)
                    model.currentSelectedShape = selectedShapeMoving
                    gc.setEffect(null)
                    gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
                    refreshCanvas()
                    state = STATE.DRAG
                }
            }
        }
    }

    init {
        // canvas width is scene width - tool width
        canvas.width = 1000.0 - 250.0
        canvas.height = 600.0

        // add canvas to pane
        children.add(canvas)

        model.canvas = canvas
        model.gc = gc

        // register with the model when we're ready to start receiving data
        model.addView(this)
    }
}