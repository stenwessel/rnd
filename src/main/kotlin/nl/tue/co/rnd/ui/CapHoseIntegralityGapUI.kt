package nl.tue.co.rnd.ui

import gurobi.GRB
import gurobi.GRBEnv
import gurobi.GRBModel
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.instance.GapInstance
import nl.tue.co.rnd.problem.CappedHoseCycleInstance
import nl.tue.co.rnd.problem.GenVpnInstance
import nl.tue.co.rnd.solver.mip.CappedHoseMipSolver
import nl.tue.co.rnd.solver.mip.GenVpnMipSolver
import nl.tue.co.rnd.util.circular
import nl.tue.co.rnd.util.permutations
import org.graphstream.graph.implementations.SingleGraph
import org.graphstream.ui.spriteManager.SpriteManager
import org.graphstream.ui.swing_viewer.SwingViewer
import org.graphstream.ui.swing_viewer.ViewPanel
import org.graphstream.ui.view.Viewer
import org.intellij.lang.annotations.Language
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.text.DecimalFormatSymbols
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CapHoseIntegralityGapUI : JFrame("RND Integrality Gap - Capped Hose") {

    private val env = GRBEnv(false)

    private val splitPane: JSplitPane
    private val iK: JSpinner
    private val iOrder: JComboBox<List<Int>>
    private val iConnectionCapacity: JTextField
    private val iTerminalCapacity: JTextField
    private val gSol: ButtonGroup
    private val iVar: JComboBox<String>
    private val cycleContainer: JPanel
    private val lIntObjVal: JLabel
    private val lFracObjVal: JLabel

    private lateinit var graph: WeightedGraph<Int>
    private lateinit var displayGraph: SingleGraph
    private lateinit var sprites: SpriteManager
    private lateinit var cycle: WeightedGraph<Int>

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


                val lPartition = JLabel("Terminal order in cycle:")
                iOrder = JComboBox<List<Int>>().apply {
                    addActionListener { loadCyclePreview() }
                    addFocusListener(object : FocusListener {
                        override fun focusGained(e: FocusEvent?) {

                        }

                        override fun focusLost(e: FocusEvent?) {
                            loadCyclePreview()
                        }
                    })
                }

                val lTerminalCapacity = JLabel("Terminal capacities:")
                iTerminalCapacity = JTextField("1,1,1,1").apply {
                    addActionListener { loadCyclePreview() }
                    addFocusListener(object : FocusListener {
                        override fun focusGained(e: FocusEvent?) {

                        }

                        override fun focusLost(e: FocusEvent?) {
                            loadCyclePreview()
                        }
                    })
                }

                val lConnectionCapacity = JLabel("Connection capacities (i,i+1):")
                iConnectionCapacity = JTextField("1,1,1,1").apply {
                    addActionListener { loadCyclePreview() }
                }

                val lK = JLabel("k =")
                iK = JSpinner(SpinnerNumberModel(4, 3, 30, 1)).apply {
                    (editor as JSpinner.DefaultEditor).textField.columns = 4
                    addChangeListener {
                        iTerminalCapacity.text = List(this.value as Int) { "1" }.joinToString(",")
                        iConnectionCapacity.text = List(this.value as Int) { "1" }.joinToString(",")
                        loadGraph()
                    }
                }

                val hGroup = layout.createSequentialGroup().apply {
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                     .addComponent(lK)
                                     .addComponent(lPartition)
                                     .addComponent(lTerminalCapacity)
                                     .addComponent(lConnectionCapacity)
                    )
                    addGroup(layout.createParallelGroup()
                                     .addComponent(iK, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                     .addComponent(iOrder)
                                     .addComponent(iTerminalCapacity)
                                     .addComponent(iConnectionCapacity)
                    )
                }
                layout.setHorizontalGroup(hGroup)

                val vGroup = layout.createSequentialGroup().apply {
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lK).addComponent(iK)
                    )
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lPartition).addComponent(iOrder)
                    )
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lTerminalCapacity).addComponent(iTerminalCapacity)
                    )
                    addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                     .addComponent(lConnectionCapacity).addComponent(iConnectionCapacity)
                    )
                }
                layout.setVerticalGroup(vGroup)

            }

            add(computePanel)

            cycleContainer = JPanel(BorderLayout()).also {
                maximumSize = Dimension(Int.MAX_VALUE, 10)
            }
            add(cycleContainer)

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
        loadCyclePreview()
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

        for (e in graph.edges()) {
            val w: Double = e["weight"] ?: continue
            if (w != 1.0) {
                e["ui.label"] = w
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
        iOrder.removeAllItems()
        for (order in (2..k).toList().permutations()) {
            iOrder.addItem(listOf(1) + order)
        }

        iVar.removeAllItems()
        iVar.addItem("x")
        for (i in 0 until k-1) {
            iVar.addItem("f between ${i+1},${(i+1)%k + 1}")
        }
        iVar.addItem("f between 1,$k")
    }

    private fun loadCyclePreview() {
        cycleContainer.removeAll()
        val order = (iOrder.model.selectedItem as? List<Int>)?.circular() ?: return
        val connectionCapacity = iConnectionCapacity.text.splitToSequence(',').map { it.trim().toDouble() }.toList()
        val terminalCapacity = iTerminalCapacity.text.splitToSequence(',').map { it.trim().toDouble() }.toList()

        val demandCycle = WeightedGraph(
                order.toSet(),
                connectionCapacity.mapIndexed { i, d -> WeightedEdge(order[i], order[i+1], d) }.toSet()
        )

        this.cycle = demandCycle

        val graph = demandCycle.toGraphstream("H", (1..order.size).toSet())

        for ((i, v) in order.withIndex()) {
            val n = graph.getNode(v.toString())
            n["ui.label"] = "${n.id}[${terminalCapacity[i]}]"
            n["x"] = cos(2*PI*(i+1)/order.size)
            n["y"] = sin(2*PI*(i+1)/order.size)
        }

        for (e in graph.edges()) {
            e["ui.label"] = e["weight"]
        }

        graph["ui.antialias"] = true
        graph["ui.quality"] = true

        graph["ui.stylesheet"] = STYLESHEET

        val viewer = SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD)
        viewer.disableAutoLayout()

        val view = viewer.addDefaultView(false) as ViewPanel

        cycleContainer.add(view, BorderLayout.CENTER)
        cycleContainer.updateUI()
    }

    private fun computeSolution() {
        val mipSolver = CappedHoseMipSolver<Int>(env, silent = true)

        val order = (iOrder.model.selectedItem as? List<Int>)?.circular() ?: return
        val terminalCapacity = order.zip(iTerminalCapacity.text.splitToSequence(',').map { it.trim().toDouble() }.toList()).toMap()
        val connectionCapacity = iConnectionCapacity.text.splitToSequence(',').map { it.trim().toDouble() }.toList()
        val instance = CappedHoseCycleInstance(graph, order, terminalCapacity, connectionCapacity)

        val (_, integerModel) = mipSolver.computeSolution(instance)
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
                CapHoseIntegralityGapUI().isVisible = true
            }
        }

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
