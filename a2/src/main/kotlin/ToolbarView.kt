import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color

internal class ToolbarView (private val model: Model) : VBox(), IView {
    private val toolbar = VBox(5.0)


    override fun updateView() {

    }

    fun setIcon(iconPath: String): ImageView {
        val icon = ImageView(Image(iconPath))
        icon.fitHeight = 20.0
        icon.setPreserveRatio(true);
        return icon
    }

    init {
        // set label properties
        val toolLabel = Label("Tools")
        toolLabel.textFill = Color.WHITE

        val selectionTool = Button("", setIcon("/selectIcon.png"))
        val eraseTool = Button("", setIcon("/eraseIcon.png"))
        val toolRow1 = HBox(5.0, selectionTool, eraseTool)
        toolRow1.alignment = Pos.CENTER

        val lineTool = Button("", setIcon("/lineToolIcon.png"))
        val circleTool = Button("", setIcon("/circleToolIcon.png"))
        val toolRow2 = HBox(5.0, lineTool, circleTool)
        toolRow2.alignment = Pos.CENTER

        val rectangleTool = Button("", setIcon("/rectangleToolIcon.png"))
        val fillTool = Button("", setIcon("/fillIcon.png"))
        val toolRow3 = HBox(5.0, rectangleTool, fillTool)
        toolRow3.alignment = Pos.CENTER

        toolbar.children.addAll(toolLabel, toolRow1, toolRow2, toolRow3)

        // on click handlers
        selectionTool.setOnMouseClicked {
            model.changeTool("select")
        }
        eraseTool.setOnMouseClicked {
            model.changeTool("erase")
        }
        lineTool.setOnMouseClicked {
            model.changeTool("line")
        }
        circleTool.setOnMouseClicked {
            model.changeTool("circle")
        }
        rectangleTool.setOnMouseClicked {
            model.changeTool("rectangle")
        }
        fillTool.setOnMouseClicked {
            model.changeTool("fill")
        }

        // add label widget to the pane
        children.add(toolbar)

        // register with the model when we're ready to start receiving data
        model.addView(this)
    }
}