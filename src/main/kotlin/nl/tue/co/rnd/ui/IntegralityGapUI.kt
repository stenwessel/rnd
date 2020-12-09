package nl.tue.co.rnd.ui

import gurobi.GRB
import gurobi.GRBEnv
import gurobi.GRBModel
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.CompactMipVpnSolver
import nl.tue.co.rnd.instance.GapInstance
import nl.tue.co.rnd.problem.GenVpnInstance
import nl.tue.co.rnd.solver.mip.GenVpnMipSolver
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.graph.implementations.SingleGraph
import org.graphstream.ui.spriteManager.SpriteManager
import org.graphstream.ui.swing_viewer.SwingViewer
import org.graphstream.ui.swing_viewer.ViewPanel
import org.graphstream.ui.view.Viewer
import org.intellij.lang.annotations.Language
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.EventQueue
import java.io.File
import java.lang.IllegalStateException
import java.text.DecimalFormatSymbols
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class IntegralityGapUI : JFrame("RND Integrality Gap") {

    private val env = GRBEnv(false)

    private val splitPane: JSplitPane
    private val iK: JSpinner
    private val iPartition: JComboBox<String>
    private val iBridge: JSpinner
    private val gSol: ButtonGroup
    private val iVar: JComboBox<String>
    private val treeContainer: JPanel
    private val lIntObjVal: JLabel
    private val lFracObjVal: JLabel

    private lateinit var graph: WeightedGraph<Int>
    private lateinit var displayGraph: SingleGraph
    private lateinit var sprites: SpriteManager
    private lateinit var tree: WeightedGraph<Int>

    private var integerModel: GRBModel? = null
    private var fractionalModel: GRBModel? = null

    private val fc: JFileChooser

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1200, 800)
        setLocationRelativeTo(null)

        val sidebar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            val computePanel = JPanel().apply {
                val layout = GroupLayout(this).apply {
                    autoCreateGaps = true
                    autoCreateContainerGaps = true
                }
                setLayout(layout)

                val lK = JLabel("k =")
                iK = JSpinner(SpinnerNumberModel(4, 3, 30, 1)).apply {
                    (editor as JSpinner.DefaultEditor).textField.columns = 4
                    addChangeListener {
                        loadGraph()
                    }
                }

                val lPartition = JLabel("Terminal partition:")
                iPartition = JComboBox<String>().apply {
                    addActionListener { loadTreePreview() }
                }

                val lBridge = JLabel("Bridge capacity:")
                iBridge = JSpinner(SpinnerNumberModel(1.0, 0.0, 100.0, 1.0)).apply {
                    val numberEditor = JSpinner.NumberEditor(this, "0.0")
                    val locale = Locale.getDefault()
                    numberEditor.format.decimalFormatSymbols = DecimalFormatSymbols(locale)
                    editor = numberEditor
                    value = 1.0

                    addChangeListener { loadTreePreview() }
                }


                val hGroup = layout.createSequentialGroup().apply {
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                     .addComponent(lK)
                                     .addComponent(lPartition)
                                     .addComponent(lBridge)
                    )
                    addGroup(layout.createParallelGroup()
                                     .addComponent(iK, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                     .addComponent(iPartition)
                                     .addComponent(iBridge, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    )
                }
                layout.setHorizontalGroup(hGroup)

                val vGroup = layout.createSequentialGroup().apply {
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lK).addComponent(iK)
                    )
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lPartition).addComponent(iPartition)
                    )
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lBridge).addComponent(iBridge)
                    )
                }
                layout.setVerticalGroup(vGroup)

            }

            add(computePanel)

            treeContainer = JPanel(BorderLayout()).also {
                maximumSize = Dimension(Int.MAX_VALUE, 10)
            }
            add(treeContainer)

            add(JButton("Compute solution").apply {
                addActionListener { computeSolution() }
            })

            add(JButton("Save screenshot").apply {
                addActionListener { saveScreenshot() }
            })

            add(JSeparator(SwingConstants.HORIZONTAL))

            val displayPanel = JPanel().apply {
                val layout = GroupLayout(this).apply {
                    autoCreateGaps = true
                    autoCreateContainerGaps = true
                }
                setLayout(layout)

                val lIntObj = JLabel("MIP objective:")
                lIntObjVal = JLabel()

                val lFracObj = JLabel("Relaxed objective:")
                lFracObjVal = JLabel()

                val lSol = JLabel("Display solution:")
                val bInt = JRadioButton("Integral").apply {
                    actionCommand = "integral"
                    addActionListener { displaySolution() }
                }
                val bFrac = JRadioButton("Fractional").apply {
                    actionCommand = "fractional"
                    addActionListener { displaySolution() }
                }
                gSol = ButtonGroup().apply { add(bInt); add(bFrac) }
                bInt.isSelected = true

                val pSol = JPanel().apply {
                    setLayout(BoxLayout(this, BoxLayout.X_AXIS))
                    add(bInt)
                    add(bFrac)
                }

                val lVar = JLabel("Variable:")
                iVar = JComboBox<String>().apply {
                    addActionListener { displaySolution() }
                }

                val hGroup = layout.createSequentialGroup().apply {
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                     .addComponent(lIntObj)
                                     .addComponent(lFracObj)
                                     .addComponent(lSol)
                                     .addComponent(lVar)
                    )
                    addGroup(layout.createParallelGroup()
                                     .addComponent(lIntObjVal)
                                     .addComponent(lFracObjVal)
                                     .addComponent(pSol)
                                     .addComponent(iVar)
                    )
                }
                layout.setHorizontalGroup(hGroup)

                val vGroup = layout.createSequentialGroup().apply {
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lIntObj).addComponent(lIntObjVal)
                    )
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lFracObj).addComponent(lFracObjVal)
                    )
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lSol).addComponent(pSol)
                    )
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lVar).addComponent(iVar)
                    )
                }
                layout.setVerticalGroup(vGroup)

            }

            add(displayPanel)
        }

        splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, null, sidebar).apply {
            dividerLocation = 800
            resizeWeight = 0.667
        }
        add(splitPane)

        fc = JFileChooser()
        fc.addChoosableFileFilter(FileNameExtensionFilter("PNG image", "png"))
        fc.addChoosableFileFilter(FileNameExtensionFilter("JPG image", "jpg"))
        fc.addChoosableFileFilter(FileNameExtensionFilter("BMP image", "bmp"))

        System.setProperty("org.graphstream.ui", "swing")
        loadGraph()
        loadTreePreview()
    }

    private fun loadGraph() {
        val k = (iK.model as SpinnerNumberModel).number as Int

        this.graph = GapInstance.constructGraph(k)
        val graph = this.graph.toGraphstream("G", (1..k).toSet())
        this.displayGraph = graph
        this.sprites = SpriteManager(graph)

        this.integerModel = null
        this.fractionalModel = null

        for (n in graph.nodes()) {
            val v: Int = n["v"] ?: continue
            n["ui.label"] = n.id

            n["x"] = when {
                v == 0 -> 0.0
                v > 0 -> 2*cos(2*PI*v/k)
                else -> -1*cos(2*PI*-v/k)
            }

            n["y"] = when {
                v == 0 -> 0.0
                v > 0 -> 2*sin(2*PI*v/k)
                else -> -1*sin(2*PI*-v/k)
            }
        }

        graph["ui.antialias"] = true
        graph["ui.quality"] = true

        graph["ui.stylesheet"] = STYLESHEET

        val viewer = SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD)
        viewer.disableAutoLayout()

        val view = viewer.addDefaultView(false)

        splitPane.leftComponent = view as ViewPanel

        // Update sidebar
        iPartition.removeAllItems()
        for (i in 1 until (0b1 shl (k-1))) {
            iPartition.addItem(i.toString(radix = 2).padStart(k, '0'))
        }

        iVar.removeAllItems()
        iVar.addItem("x")
        for (i in 1..k) {
            for (j in (i+1)..k) {
                iVar.addItem("f between $i,$j")
            }
        }
    }

    private fun loadTreePreview() {
        treeContainer.removeAll()
        val partition = iPartition.model.selectedItem as? String ?: return
        val bridgeCapacity = (iBridge.model as? SpinnerNumberModel)?.number as? Double ?: return

        val left = partition.mapIndexedNotNull { i, c -> if (c == '0') i+1 else null }
        val right = partition.mapIndexedNotNull { i, c -> if (c == '1') i+1 else null }

        val demandTree = WeightedGraph(
                (-2..partition.length).toSet() - 0,
                partition.mapIndexed { j, side -> WeightedEdge(j + 1, side.toInt() - ZERO_CHAR - 2, 1.0) }.toSet() + WeightedEdge(-1, -2, bridgeCapacity)
        )

        this.tree = demandTree

        val graph = demandTree.toGraphstream("T", (1..partition.length).toSet())

        for (n in graph.nodes()) {
            val v: Int = n["v"] ?: continue

            if (v > 0) n["ui.label"] = n.id

            n["x"] = when {
                v == -1 -> 0.5
                v == -2 -> -0.5
                v > 0 && partition[v-1] == '0' -> 0.7*cos((PI/2 + PI/8) + 6*PI/16/left.size + 6*PI/8*(left.indexOf(v))/left.size) - 0.5
                else -> 0.7*cos((PI/2 - PI/8) - 6*PI/16/right.size - 6*PI/8*(right.indexOf(v))/right.size) + 0.5
            }

            n["y"] = when {
                v < 0 -> 0.0
                v > 0 && partition[v-1] == '0' -> 0.7*sin((PI/2 + PI/8) + 6*PI/16/left.size + 6*PI/8*(left.indexOf(v))/left.size)
                else -> 0.7*sin((PI/2 - PI/8) - 6*PI/16/right.size - 6*PI/8*(right.indexOf(v))/right.size)
            }
        }

        val bridge = graph.getEdge("{-1,-2}")
        bridge["ui.label"] = "$bridgeCapacity"

        graph["ui.antialias"] = true
        graph["ui.quality"] = true

        graph["ui.stylesheet"] = STYLESHEET

        val viewer = SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD)
        viewer.disableAutoLayout()

        val view = viewer.addDefaultView(false) as ViewPanel

        treeContainer.add(view, BorderLayout.CENTER)
        treeContainer.updateUI()
    }

    private fun computeSolution() {
        val k = (iK.model as SpinnerNumberModel).number as Int

        val mipSolver = GenVpnMipSolver<Int>(env, silent = true)
        val (_, integerModel) = mipSolver.computeSolution(GenVpnInstance(graph, tree, (1..k).toSet()))
        this.integerModel = integerModel

        val lp = integerModel.relax()
        this.fractionalModel = lp

        lp.optimize()

        lIntObjVal.text = integerModel[GRB.DoubleAttr.ObjVal].toString()
        lFracObjVal.text = lp[GRB.DoubleAttr.ObjVal].toString()

        displaySolution()
    }

    private fun displaySolution() {
        val model = when (gSol.selection.actionCommand) {
            "integral" -> integerModel ?: return
            "fractional" -> fractionalModel ?: return
            else -> return
        }

        val variable = iVar.model.selectedItem as? String ?: return

        if (variable == "x") {
            displayX(model)
        }
        else {
            val (i, j) = variable.split(' ').last().split(',').map { it.toInt() }
            displayFlow(model, i, j)
        }
    }

    private fun displayX(model: GRBModel) {
        resetDisplay()

        for ((u, v) in graph.edges) {
            val x = model.getVarByName("x[u=$u,v=$v]")[GRB.DoubleAttr.X]
            if (x > EPSILON) {
                val e = displayGraph.getEdge("{$u,$v}")
                e["ui.label"] = "%.2f".format(x)
                e["ui.class"] = "highlight"
            }
        }
    }

    private fun displayFlow(model: GRBModel, i: Int, j: Int) {
        resetDisplay()

        for ((u, v) in graph.edges) {
            val fuv = model.getVarByName("f[u=$u,v=$v,i=$i,j=$j]")[GRB.DoubleAttr.X]
            val fvu = model.getVarByName("f[u=$v,v=$u,i=$i,j=$j]")[GRB.DoubleAttr.X]

            if (fuv > EPSILON || fvu > EPSILON) {
                val s = sprites.addSprite("f[$u,$v]")
                s.attachToEdge("{$u,$v}")
                s["ui.class"] = "flo"
                s.setPosition(1.0)

            }

            if (fuv > EPSILON) {
                val a = sprites.addSprite("a[$u,$v]")
                a.attachToEdge("{$u,$v}")
                a.setPosition(0.5)
                a["ui.class"] = "arro, too"
                a["ui.label"] = "%.2f".format(fuv)
            }

            if (fvu > EPSILON) {
                val a = sprites.addSprite("a[$v,$u]")
                a.attachToEdge("{$u,$v}")
                a.setPosition(0.5)
                a["ui.class"] = "arro, fro"
                a["ui.label"] = "%.2f".format(fvu)
            }
        }
    }

    private fun resetDisplay() {
        for (e in displayGraph.edges()) {
            e.removeAttribute("ui.label")
            e.removeAttribute("ui.class")
        }

        for (s in sprites.sprites().map { it.id }) {
            sprites.removeSprite(s)
        }
    }

    private fun saveScreenshot() {
        val result = fc.showSaveDialog(this)

        if (result != JFileChooser.APPROVE_OPTION) return

        this.displayGraph.setAttribute("ui.screenshot", fc.selectedFile.absolutePath)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            EventQueue.invokeLater {
                IntegralityGapUI().isVisible = true
            }
        }

        private const val ZERO_CHAR = '0'.toInt()

        private const val EPSILON = 10E-5

        @Language("CSS")
        private val STYLESHEET = """
                node {
                    text-alignment: above;
                    text-offset: 0px, -5px;
                    text-size: 13;
                    text-background-mode: plain;
                    text-background-color: white;
                }
                
                node.terminal {
                    shape: box;
                }
                
                edge {
                    text-alignment: above;
                    text-background-mode: plain;
                    text-background-color: white;
                    text-size: 13;
                    z-index: 0;
                }
                
                sprite {
                    text-background-mode: plain;
                    text-background-color: white;
                    text-size: 13;
                }
                
                edge.highlight {
                    fill-color: red;
                    size: 3px;
                    text-color: red;
                }
                
                sprite.flo {
                    fill-color: red;
                    size: 3px;
                    z-index: 1;
                    shape: flow;
                }
                
                sprite.arro {
                    fill-color: red;
                    size: 15px;
                    z-index: 1;
                    shape: arrow;
                    text-color: red;
                }
                
                sprite.fro {
                    sprite-orientation: from;
                    text-alignment: above;
                }
                
                sprite.too {
                    sprite-orientation: to;
                    text-alignment: under;
                }
                
            """.trimIndent()
    }
}
