import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ColorPicker
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle

internal class ToolbarPropertyView (private val model: Model) : VBox(), IView {
    private var toolbarOptions = VBox(5.0)

    private val childrenMapIndex = mapOf(
        "toolOptionRowLineColour" to 1,
        "toolOptionRowFillColour" to 2,
        "toolOptionRowThickness" to 4,
        "toolOptionRowStyle" to 6,
    )

    // When notified by the model that things have changed,
    // update to display the new value
    override fun updateView() {

        // for line tool, disable everything but the fill
        if (model.isToolChanged || model.selectedTool == "select") {
            toolbarOptions.children.remove(0, toolbarOptions.children.size)
            setUpToolOptions()
        }

        // only disable this tool when:
        // 1. tool is select, but no shape is clicked
        // 2. tool is erase
        toolbarOptions.isDisable = (model.selectedTool == "select" && model.currentSelectedShape == null || model.selectedTool == "erase")

        if (model.selectedTool == "select" && model.currentSelectedShape != null) {
            // updates the value shown on the line colour picker
            val lineColourIndex = childrenMapIndex["toolOptionRowLineColour"]
            if (lineColourIndex != null) {
                val hBox: HBox = toolbarOptions.children[lineColourIndex] as HBox
                val linePickerHBox: HBox = hBox.children[1] as HBox
                val linePicker: ColorPicker = linePickerHBox.children[0] as ColorPicker
                linePicker.value = model.getLineColour() as Color?
            }

            val fillColourIndex = childrenMapIndex["toolOptionRowFillColour"]
            if (fillColourIndex != null) {
                val hBox: HBox = toolbarOptions.children[fillColourIndex] as HBox
                val fillPickerHBox: HBox = hBox.children[1] as HBox
                val fillPicker: ColorPicker = fillPickerHBox.children[0] as ColorPicker
                fillPicker.value = model.getFillColour() as Color?
            }
        }
    }

    private fun setFillColour(): Rectangle {
        val fillColour = Rectangle(35.0, 10.0)
        fillColour.fill = Color.BLACK
        return fillColour
    }

    // for tool button display only
    private fun setLineThickness(thickness: Double): Line {
        val line = Line()
        line.strokeWidth = thickness
        line.endX = line.startX + 35
        return line
    }

    // for tool button display only
    private fun setLineStyle(strokeArray: ArrayList<Double>): Line {
        val line = Line()
        line.endX = line.startX + 40
        line.strokeDashArray.addAll(strokeArray)
        return line
    }

    fun getLineColorRow(): HBox {
        // line colour
        val lineColourOption = VBox(setLineThickness(1.5))
        lineColourOption.alignment = Pos.CENTER

        val lineColorPickerBox = HBox()
        val lineColorPicker = ColorPicker(Color.BLACK)
        lineColorPickerBox.children.add(lineColorPicker)
        val toolOptionRowLineColour = HBox(10.0, lineColourOption, lineColorPickerBox)
        toolOptionRowLineColour.alignment = Pos.CENTER

        lineColorPicker.setOnAction {
            model.changeToolProperty("lineColour", model.getColorHexString(lineColorPicker.value.toString()))
        }

        // disable if fill tool
        if (model.selectedTool == "fill") {
            toolOptionRowLineColour.isDisable = true
        }

        return  toolOptionRowLineColour
    }

    fun getfillColorRow(): HBox {
        //fill options
        val fillColourOption = VBox(setFillColour())
        fillColourOption.alignment = Pos.CENTER

        val fillColorPickerBox = HBox()
        val fillColorPicker = ColorPicker(Color.WHITE)
        fillColorPickerBox.children.add(fillColorPicker)
        val toolOptionRowFillColour = HBox(10.0, fillColourOption, fillColorPickerBox)
        toolOptionRowFillColour.alignment = Pos.CENTER

        fillColorPicker.setOnAction {
            model.changeToolProperty("fillColour", model.getColorHexString(fillColorPicker.value.toString()))
        }

        // disable if is a line
        if (model.selectedTool == "line" || (model.selectedTool == "select" && model.currentSelectedShape is Line)) {
            toolOptionRowFillColour.isDisable = true
        }

        return toolOptionRowFillColour
    }

    fun getLineThicknessRow(): HBox {
        // line thickness options
        val lineThicknessNormal = Button("", setLineThickness(1.0))
        val lineThicknessSmall = Button("", setLineThickness(4.0))
        val lineThicknessMedium = Button("", setLineThickness(7.0))
        val lineThicknessLarge = Button("", setLineThickness(10.0))
        val toolOptionRowThickness = HBox(5.0, lineThicknessNormal, lineThicknessSmall, lineThicknessMedium, lineThicknessLarge)
        toolOptionRowThickness.alignment = Pos.CENTER

        lineThicknessNormal.setOnMouseClicked {
            model.changeToolProperty("lineThickness", "1.0")
        }
        lineThicknessSmall.setOnMouseClicked {
            model.changeToolProperty("lineThickness", "4.0")
        }
        lineThicknessMedium.setOnMouseClicked {
            model.changeToolProperty("lineThickness", "7.0")
        }
        lineThicknessLarge.setOnMouseClicked {
            model.changeToolProperty("lineThickness", "10.0")
        }

        // disable if fill tool
        if (model.selectedTool == "fill") {
            toolOptionRowThickness.isDisable = true
        }

        return toolOptionRowThickness
    }

    fun getLineStyleRow(): HBox {
        val lineStyleNormal = Button("", setLineStyle(arrayListOf(1.0)))
        val lineStyleSmallDotted = Button("", setLineStyle(arrayListOf(3.0)))
        val lineStyleMediumDotted = Button("",  setLineStyle(arrayListOf(7.0, 2.0)))
        val lineStyleLongDotted = Button("", setLineStyle(arrayListOf(10.0, 5.0)))
        val toolOptionRowStyle = HBox(5.0, lineStyleNormal, lineStyleSmallDotted, lineStyleMediumDotted, lineStyleLongDotted)
        toolOptionRowStyle.alignment = Pos.CENTER

        lineStyleNormal.setOnMouseClicked {
            model.changeToolProperty("lineStyle", "1.0")
        }
        lineStyleSmallDotted.setOnMouseClicked {
            model.changeToolProperty("lineStyle", "15.0")
        }
        lineStyleMediumDotted.setOnMouseClicked {
            model.changeToolProperty("lineStyle", "40.0,20.0")
        }
        lineStyleLongDotted.setOnMouseClicked {
            model.changeToolProperty("lineStyle", "50.0,40.0")
        }

        // disable if fill tool
        if (model.selectedTool == "fill") {
            toolOptionRowStyle.isDisable = true
        }

        return toolOptionRowStyle
    }

    fun setUpToolOptions() {
        // line/fill color options
        val colourLabel = Label("Colour Options")
        colourLabel.textFill = Color.WHITE
        val toolOptionRowLineColour = getLineColorRow()
        val toolOptionRowFillColour = getfillColorRow()

        // line thickness options
        val lineThicknessLabel = Label("Line Thickness")
        lineThicknessLabel.textFill = Color.WHITE
        val toolOptionRowThickness = getLineThicknessRow()

        // line style options
        val lineStyleLabel = Label("Line Style")
        lineStyleLabel.textFill = Color.WHITE
        val toolOptionRowStyle = getLineStyleRow()

        toolbarOptions.children.addAll(colourLabel, toolOptionRowLineColour, toolOptionRowFillColour, lineThicknessLabel, toolOptionRowThickness, lineStyleLabel, toolOptionRowStyle)
    }

    init {
        setUpToolOptions()

        // add label widget to the pane
        children.add(toolbarOptions)

        // register with the model when we're ready to start receiving data
        model.addView(this)
    }
}