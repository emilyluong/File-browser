import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ColorPicker
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle

internal class ToolbarPropertyView (private val model: Model) : VBox(), IView {
    private val toolbarOptions = VBox(5.0)

    private val childrenMapIndex = mapOf(
        "toolOptionRowLineColour" to 1,
        "toolOptionRowFillColour" to 2,
        "toolOptionRowThickness" to 4,
        "toolOptionRowStyle" to 6,
    )

    // When notified by the model that things have changed,
    // update to display the new value
    override fun updateView() {
        // only disable this tool when:
        // 1. tool is select, but no shape is clicked
        // 2. tool is erase
        toolbarOptions.isDisable = (model.selectedTool == "select" && model.currentSelectedShape == null || model.selectedTool == "erase")

        println("is toolbar options disabled " + toolbarOptions.isDisable + " " + model.selectedTool + " " + model.currentSelectedShape)
        if (model.selectedTool == "select" && model.currentSelectedShape != null) {
            // updates the value shown on the line colour picker
            val lineColourIndex = childrenMapIndex["toolOptionRowLineColour"]
            if (lineColourIndex != null) {
                val hBox: HBox = toolbarOptions.children[lineColourIndex] as HBox
                val linePickerHBox: HBox = hBox.children[1] as HBox
                val linePicker: ColorPicker = linePickerHBox.children[0] as ColorPicker
                linePicker.value = model.getLineColour() as Color?
            }

            println("style " + model.selectedToolProperty)
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

    init {

        val colourLabel = Label("Colour Options")
        colourLabel.textFill = Color.WHITE

        // line colour
        val lineColourOption = VBox(setLineThickness(1.5))
        lineColourOption.alignment = Pos.CENTER

        val lineColorPickerBox = HBox()
        val lineColorPicker = ColorPicker(Color.BLACK)
        lineColorPickerBox.children.add(lineColorPicker)
        val toolOptionRowLineColour = HBox(10.0, lineColourOption, lineColorPickerBox)
        toolOptionRowLineColour.alignment = Pos.CENTER

        //fill options
        val fillColourOption = VBox(setFillColour())
        fillColourOption.alignment = Pos.CENTER

        val fillColorPickerBox = HBox()
        val fillColorPicker = ColorPicker(Color.WHITE)
        fillColorPickerBox.children.add(fillColorPicker)
        val toolOptionRowFillColour = HBox(10.0, fillColourOption, fillColorPickerBox)
        toolOptionRowFillColour.alignment = Pos.CENTER

        // line thickness options
        val lineThicknessLabel = Label("Line Thickness")
        lineThicknessLabel.textFill = Color.WHITE
        val lineThicknessNormal = Button("", setLineThickness(1.0))
        val lineThicknessSmall = Button("", setLineThickness(4.0))
        val lineThicknessMedium = Button("", setLineThickness(7.0))
        val lineThicknessLarge = Button("", setLineThickness(10.0))
        val toolOptionRowThickness = HBox(5.0, lineThicknessNormal, lineThicknessSmall, lineThicknessMedium, lineThicknessLarge)
        toolOptionRowThickness.alignment = Pos.CENTER

        // line style options
        val lineStyleLabel = Label("Line Style")
        lineStyleLabel.textFill = Color.WHITE
        val lineStyleNormal = Button("", setLineStyle(arrayListOf(1.0)))
        val lineStyleSmallDotted = Button("", setLineStyle(arrayListOf(3.0)))
        val lineStyleMediumDotted = Button("",  setLineStyle(arrayListOf(7.0, 2.0)))
        val lineStyleLongDotted = Button("", setLineStyle(arrayListOf(10.0, 5.0)))
        val toolOptionRowStyle = HBox(5.0, lineStyleNormal, lineStyleSmallDotted, lineStyleMediumDotted, lineStyleLongDotted)
        toolOptionRowStyle.alignment = Pos.CENTER
        toolbarOptions.children.addAll(colourLabel, toolOptionRowLineColour, toolOptionRowFillColour, lineThicknessLabel, toolOptionRowThickness, lineStyleLabel, toolOptionRowStyle)

        // color chooser on actions
        lineColorPicker.setOnAction {
            model.changeToolProperty("lineColour", model.getColorHexString(lineColorPicker.value.toString()))
        }
        fillColorPicker.setOnAction {
            model.changeToolProperty("fillColour", model.getColorHexString(fillColorPicker.value.toString()))
        }

        // line thickness on mouse click
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

        // line style on mouse click
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

        // add label widget to the pane
        children.add(toolbarOptions)

        // register with the model when we're ready to start receiving data
        model.addView(this)
    }
}