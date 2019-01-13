package net.totalbits.game4096

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import java.lang.Math
import java.lang.System
import android.view.View
import android.graphics.Color
import java.util.*
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object MyConfig {
    private var share: SharedPreferences? = null
    private val configPath = "appConfig"

    fun getProperties(context: Context): SharedPreferences? {
        try {
            share = context.getSharedPreferences(configPath, Context.MODE_PRIVATE)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return share
    }

    fun setProperties(context: Context, keyName: String, keyValue: Int): Boolean {
        try {
            share = context.getSharedPreferences(configPath, Context.MODE_PRIVATE)
            val editor = share!!.edit()
            editor.putInt(keyName, keyValue)
            editor.commit()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("setPropertiesError", e.toString())
            return false
        }
        return true
    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        readConfig()
        createSpinners()
        createBricks()
        newGame()

        val btn = findViewById<Button>(R.id.button_newgame)
        btn.setOnClickListener { newGame() }
    }

    data class BrickInfo(val x: Int, val y: Int, var value: Int)

    private var BRICK_SIZE = 4

    private var bricks = arrayOfNulls<Button>(BRICK_SIZE * BRICK_SIZE)

    private fun createBricks() {
        val resources = this.resources
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels
        val brickWidth = screenWidth / BRICK_SIZE

        val fl = findViewById<FrameLayout>(R.id.brick_area)
        for (y in 0 until BRICK_SIZE) {
            for (x in 0 until BRICK_SIZE) {
                val button = Button(this)
                val params = FrameLayout.LayoutParams(brickWidth, brickWidth)
                params.leftMargin = brickWidth * x
                params.topMargin = brickWidth * y
                button.layoutParams = params
                button.textSize = 28.0f
                button.tag = BrickInfo(x, y, 0)
                button.setOnClickListener { brickClicked(button) }
                bricks[y * BRICK_SIZE + x] = button
                fl.addView(button)
            }
        }
    }

    private val modes = arrayOf("4096", "士兵突击", "权力的游戏", "历史的进程", "永恒的时间")

    private val literals = arrayOf(arrayOf("", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096"),
            arrayOf("", "民兵", "士兵", "班长", "排长", "连长", "营长", "团长", "旅长", "师长", "军长", "司令", "元帅"),
            arrayOf("", "办事员", "科员", "副科", "正科", "副处", "正处", "副司", "正司", "副部", "正部", "总理", "总统"),
            arrayOf("", "夏", "商", "周", "秦", "汉", "晋", "隋", "唐", "宋", "元", "明", "清"),
            arrayOf("", "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"))

    private var gameMode = 0
    private var difficulty = 0

    private fun readConfig() {
        val proper = MyConfig.getProperties(this)
        gameMode = proper!!.getInt("GameMode", 0)
        difficulty = proper.getInt("Difficulty", 0)
    }

    private fun saveConfig() {
        MyConfig.setProperties(this, "GameMode", gameMode)
        MyConfig.setProperties(this, "Difficulty", difficulty)
    }

    private fun createSpinners() {
        val spMode = findViewById<Spinner>(R.id.select_mode)
        val modeAdapter = ArrayAdapter<String>(this, R.layout.layout_spinner, modes)
        modeAdapter.setDropDownViewResource(R.layout.layout_spinner)
        spMode.adapter = modeAdapter
        spMode.setSelection(gameMode)
        spMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                gameMode = position
                saveConfig()
                refreshBricks()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }
        }

        val spDiff = findViewById<Spinner>(R.id.select_difficulty)
        val diffs = arrayListOf<String>()
        diffs.add("容易")
        diffs.add("中等")
        diffs.add("困难")
        val diffAdapter = ArrayAdapter<String>(this, R.layout.layout_spinner, diffs)
        diffAdapter.setDropDownViewResource(R.layout.layout_spinner)
        spDiff.adapter = diffAdapter
        spDiff.setSelection(difficulty)
        spDiff.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                difficulty = position
                saveConfig()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }
        }
    }

    enum class RunningStatus {
        Normal, Failed, Success
    }

    private var running: RunningStatus = RunningStatus.Failed
    private var score = 0
    private var beginning: Long = -1

    private fun newGame() {
        //Toast.makeText(this, "new game clicked", Toast.LENGTH_LONG).show()
        // reset status
        running = RunningStatus.Normal
        updateScore(-1)
        updateTime(true)
        beginning = System.currentTimeMillis()
        pre = null

        // new 2 bricks
        for (brick: Button? in bricks) {
            val info = brick!!.tag as BrickInfo
            if ((info.y  == BRICK_SIZE - 1)) {
                info.value = info.x + 1
            } else {
                info.value = 0
            }
        }
        refreshBricks()
    }

    private var pre: Button? = null
    private var cur: Button? = null

    private fun brickClicked(btn: Button) {
        //Toast.makeText(this, btn.tag.toString() + " is clicked", Toast.LENGTH_SHORT).show()
        if (running == RunningStatus.Failed) {
            Toast.makeText(this, "Game over, please start a new game!", Toast.LENGTH_LONG).show()
            return
        } else if (running == RunningStatus.Success) {
            Toast.makeText(this, "You have won the game, please start a new game!", Toast.LENGTH_LONG).show()
            return
        }

        if (pre == null) {
            // first button
            pre = btn
            return
        } else if (pre == btn) {
            // same button
            return
        } else {
            // first -> second button, same line
            cur = btn
            val a = pre!!.tag as BrickInfo
            val b = cur!!.tag as BrickInfo
            if (a.x == b.x || a.y == b.y) {
                updateTime()
                var d = b.x - a.x + b.y - a.y
                d /= Math.abs(d) // move direction -1 or 1
                tryAction(if (a.x == b.x) a.x else -1, if (a.y == b.y) a.y else -1, d)
                running = judge()
                if (running == RunningStatus.Failed) {
                    // game over
                    Toast.makeText(this, "Game over, please start a new game!", Toast.LENGTH_LONG).show()
                } else if (running == RunningStatus.Success) {
                    // success
                    Toast.makeText(this, "You have won the game, please start a new game!", Toast.LENGTH_LONG).show()
                }
                pre = null
            } else {
                pre = btn
            }
        }
    }

    private fun tryAction(x: Int, y: Int, d: Int) {
        if (x == -1 && y in 0 until BRICK_SIZE) {
            //Toast.makeText(this, "Move row $y", Toast.LENGTH_LONG).show()
            for (i in 0 until BRICK_SIZE) {
                moveRow(i, d, i == y)
            }
        } else if (y == -1 && x in 0 until BRICK_SIZE) {
            //Toast.makeText(this, "Move column $x", Toast.LENGTH_LONG).show()
            for (i in 0 until BRICK_SIZE) {
                moveColumn(i, d, i == x)
            }
        } else {
            assert(false)
        }
        refreshBricks()
    }

    private fun moveRow(i: Int, d: Int, bomb: Boolean) {
        var bombed = false
        // bomb
        if (bomb) {
            val t = pre!!.tag as BrickInfo
            assert(t.y == i)
            val n = t.y * BRICK_SIZE + t.x + d
            val next = bricks[n]!!.tag as BrickInfo
            // bomb!!
            if ((t.value == next.value) && (t.value > 0)) {
                updateScore(Math.pow(2.0, t.value.toDouble()).toInt())
                next.value++
                t.value = 0
                bombed = true
            }
        }

        // move on the direction
        // collect non-zero elements
        val values = mutableListOf<Int>()
        for (j in 0 until BRICK_SIZE) {
            val n = i * BRICK_SIZE + j
            val b = bricks[n]!!.tag as BrickInfo
            if (b.value > 0) {
                values.add(b.value)
            }
        }
        if (values.size == BRICK_SIZE) {
            // nothing to do
        } else {
            if (bomb && !bombed) {
                if (d > 0) {
                    values.add(0, newBrick())
                } else {
                    values.add(newBrick())
                }
            }
            //
            while (values.size < BRICK_SIZE) {
                if (d > 0) {
                    values.add(0, 0)
                } else {
                    values.add(0)
                }
            }
        }
        // re-assigned
        for (j in 0 until BRICK_SIZE) {
            val n = i * BRICK_SIZE + j
            val b = bricks[n]!!.tag as BrickInfo
            b.value = values[j]
        }
    }

    private fun moveColumn(i: Int, d: Int, bomb: Boolean) {
        var bombed = false
        // bomb
        if (bomb) {
            val t = pre!!.tag as BrickInfo
            assert(t.x == i)
            val n = (t.y + d) * BRICK_SIZE + t.x
            val next = bricks[n]!!.tag as BrickInfo
            // bomb!!
            if ((t.value == next.value) && (t.value > 0)) {
                updateScore(Math.pow(2.0, t.value.toDouble()).toInt())
                next.value++
                t.value = 0
                bombed = true
            }
        }

        // move on the direction
        // collect non-zero elements
        val values = mutableListOf<Int>()
        for (j in 0 until BRICK_SIZE) {
            val n = j * BRICK_SIZE + i
            val b = bricks[n]!!.tag as BrickInfo
            if (b.value > 0) {
                values.add(b.value)
            }
        }
        if (values.size == BRICK_SIZE) {
            // nothing to do
        } else {
            if (bomb && !bombed) {
                if (d > 0) {
                    values.add(0, newBrick())
                } else {
                    values.add(newBrick())
                }
            }
            //
            while (values.size < BRICK_SIZE) {
                if (d > 0) {
                    values.add(0, 0)
                } else {
                    values.add(0)
                }
            }
        }
        // re-assigned
        for (j in 0 until BRICK_SIZE) {
            val n = j * BRICK_SIZE + i
            val b = bricks[n]!!.tag as BrickInfo
            b.value = values[j]
        }
    }

    // false is failed, true is OK
    private fun judge(): RunningStatus {
        var isNormal = false
        for (y in 0 until BRICK_SIZE) {
            for (x in 0 until BRICK_SIZE) {
                val brick = bricks[y * BRICK_SIZE + x]!!.tag as BrickInfo
                if (brick.value == 0) {
                    isNormal = true
                } else if (brick.value == 12) {
                    return RunningStatus.Success
                }
                if (y + 1 < BRICK_SIZE) {
                    val t = bricks[(y + 1) * BRICK_SIZE + x]!!.tag as BrickInfo
                    if (brick.value == t.value) {
                        isNormal = true
                    }
                }
                if (x + 1 < BRICK_SIZE) {
                    val t = bricks[y * BRICK_SIZE + x + 1]!!.tag as BrickInfo
                    if (brick.value == t.value) {
                        isNormal = true
                    }
                }
            }
        }
        if (isNormal) {
            return RunningStatus.Normal
        } else {
            return RunningStatus.Failed
        }
    }

    private fun newBrick(): Int {
        val numbers = mutableListOf<Int>()
        for (brick in bricks) {
            val t = brick!!.tag as BrickInfo
            if (t.value > 0) {
                numbers.add(t.value)
            }
        }
        assert(numbers.isNotEmpty())
        val maxNum = numbers.max()!!
        numbers.removeAll { it == maxNum }

        val candidates = arrayListOf<Int>()
        candidates.addAll(1 until maxNum)
        when (difficulty) {
            0 -> candidates.addAll(numbers)
            1 -> candidates.addAll(numbers.distinct())
            2 -> {
                if (occupied() < (BRICK_SIZE * BRICK_SIZE / 2)) {
                    for (x in numbers.distinct().shuffled()) {
                        if (candidates.size > 2) {
                            candidates.remove(x)
                        } else {
                            break
                        }
                    }
                } else {
                    candidates.addAll(numbers.distinct())
                }
            }
        }

        val rand = Random()
        candidates.shuffle()
        return candidates[rand.nextInt(candidates.size)]
    }

    private fun occupied(): Int {
        var n = 0
        for (brick in bricks) {
            val t = brick!!.tag as BrickInfo
            if (t.value > 0) {
                n++
            }
        }
        return n
    }

    private val colors = longArrayOf(0xFFCC0033, 0xFF66CC99, 0xFF336666, 0xFF996633, 0xFFCCCC33, 0xFF336633, 0xFF990033,
            0xFF333366, 0xFF669999, 0xFF996600, 0xFF993333, 0xFFCC9966, 0xFF003300)

    private fun argb32Color(index: Int): Int {
        val a = ((colors[index] shr 24) and 0xFF)
        val r = ((colors[index] shr 16) and 0xFF)
        val g = ((colors[index] shr 8) and 0xFF)
        val b = ((colors[index] shr 0) and 0xFF)
        return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }

    private fun refreshBricks() {
        for (brick: Button? in bricks) {
            val info = brick!!.tag as BrickInfo
            brick.text = literals[gameMode][info.value]
            brick.setTextColor(argb32Color(info.value))
        }
    }

    private fun updateScore(added: Int) {
        if (added > 0) {
            score += added
        } else {
            score = 0
        }
        val txt = findViewById<TextView>(R.id.display_score)
        txt.text = String.format(" %04d", score)
    }

    private fun updateTime(reset: Boolean = false) {
        val txt = findViewById<TextView>(R.id.display_time)
        if (reset) {
            txt.text = "00:00"
        } else {
            var dur = System.currentTimeMillis() - beginning
            dur /= 1000
            val s = dur % 60
            val m = dur / 60
            txt.text = String.format("%02d:%02d", m, s)
        }
    }
}
