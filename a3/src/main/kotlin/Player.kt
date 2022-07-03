import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image

class Player {

    val screenWidth = 1300.0
    val screenHeight = 800.0

    val playerImage = Image("/images/player.png")
    val playerImageWidth = 60.0
    val playerImageHeight = 40.0

    var positionX = screenWidth/2
    var positionY = screenHeight-45

    var playerSpeed = 0.0

    var isMoveRight = false
    var isMoveLeft = false

    fun moveRight(gc: GraphicsContext) {
        if (positionX + playerImageWidth < screenWidth - 10) {
            positionX += playerSpeed
            gc.clearRect(0.0, 0.0, screenWidth, screenHeight)
            draw(gc)
        }
    }

    fun moveLeft(gc: GraphicsContext) {
        if (positionX > 10) {
            positionX -= playerSpeed
            gc.clearRect(0.0, 0.0, screenWidth, screenHeight)
            draw(gc)
        }
    }

    fun shoot(bulletSpeed: Double): PlayerBullet {
        val posX = positionX + playerImageWidth / 2 - 3
        val posY = screenHeight - playerImageHeight - 25  // 25 is height of player bullet

        return PlayerBullet(posX, posY, bulletSpeed)
    }

    fun isCollided(enemy: Enemy): Boolean {
        val enemyBottomPosY = enemy.positionY + enemy.enemyImageHeight
        val enemyLeftPosX = enemy.positionX
        val enemyRightPosX = enemy.positionX + enemy.enemyImageWidth

        val playerTopPosY = positionY
        val playerLeftPosX = positionX
        val playerRightPosX = positionX + playerImageWidth

        return enemyBottomPosY > playerTopPosY && enemyLeftPosX < playerRightPosX && enemyRightPosX > playerLeftPosX
    }

    fun draw(gc: GraphicsContext) {
        gc.drawImage(playerImage, positionX, positionY, playerImageWidth, playerImageHeight)
    }
}