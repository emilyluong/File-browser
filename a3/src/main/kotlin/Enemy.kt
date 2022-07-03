import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image

class Enemy {

    var enemyImage: Image? = null
    val enemyImageWidth = 60.0
    val enemyImageHeight = 40.0

    var positionX = 0.0
    var positionY = 0.0

    var enemySpeed = 0.0
    var enemyVerticalSpeed = 0.0

    var isKilled = false

    constructor(enemy: Image, posX: Double, posY: Double, speed: Double, verticalSpeed: Double) {
        positionX = posX
        positionY = posY
        enemySpeed = speed
        enemyVerticalSpeed = verticalSpeed
        enemyImage = enemy
    }

    fun moveEnemyToRight() {
        positionX += enemySpeed
    }

    fun moveEnemyToLeft() {
        positionX -= enemySpeed
    }

    fun moveEnemyDown(gc: GraphicsContext) {
        positionY += enemyVerticalSpeed
    }

    fun draw(gc: GraphicsContext) {
        gc.drawImage(enemyImage, positionX, positionY, enemyImageWidth, enemyImageHeight)
    }
}