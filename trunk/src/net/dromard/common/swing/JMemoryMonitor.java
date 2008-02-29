package net.dromard.common.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


/**
 * Tracks Memory allocated & used, displayed in graph form.
 */
public class JMemoryMonitor extends JPanel {

    protected static final long serialVersionUID = -347102535481575324L;
    protected static JCheckBox dateStampCB = new JCheckBox("Output Date Stamp");
    protected Surface surf;
    protected JPanel controls;
    protected boolean doControls;
    protected JTextField tf;

    public JMemoryMonitor() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder(new EtchedBorder(), "Memory Monitor"));
        add(surf = new Surface());
        controls = new JPanel();
        controls.setPreferredSize(new Dimension(135,80));
        Font font = new Font("serif", Font.PLAIN, 10);
        JLabel label = new JLabel("Sample Rate");
        label.setFont(font);
        label.setForeground(Color.black);
        controls.add(label);
        tf = new JTextField("1000");
        tf.setPreferredSize(new Dimension(45,20));
        controls.add(tf);
        controls.add(label = new JLabel("ms"));
        label.setFont(font);
        label.setForeground(Color.black);
        controls.add(dateStampCB);
        dateStampCB.setFont(font);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
               removeAll();
               doControls = !doControls;
               if (doControls) {
                   surf.stop();
                   add(controls);
               } else {
                   try {
                       surf.sleepAmount = Long.parseLong(tf.getText().trim());
                   } catch (Exception ex) {
                       ex.printStackTrace();
                   }
                   surf.start();
                   add(surf);
               }
               validate();
               repaint();
            }
        });
        surf.start();
    }


    private class Surface extends JPanel implements Runnable {

        protected static final long serialVersionUID = -4298072680026629003L;
        protected Thread thread;
        protected long sleepAmount = 1000;
        protected int w, h;
        protected BufferedImage bimg;
        protected Graphics2D big;
        protected Font font = new Font("Times New Roman", Font.PLAIN, 11);

        protected int columnInc;
        protected int pts[];
        protected int ptNum;
        protected int ascent, descent;
        //private float freeMemory, totalMemory;
        //private Rectangle graphOutlineRect = new Rectangle();
        protected Rectangle2D mfRect = new Rectangle2D.Float();
        protected Rectangle2D muRect = new Rectangle2D.Float();
        protected Line2D graphLine = new Line2D.Float();
        protected Color graphColor = new Color(46, 139, 87);
        protected Color mfColor = new Color(0, 100, 0);
        protected String usedStr;
      

        public Surface() {
            setBackground(Color.black);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (thread == null) start(); else stop();
                }
            });
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            return new Dimension(135,80);
        }
            
        public void paint(Graphics g) {

            if (big == null) {
                return;
            }

            big.setBackground(getBackground());
            big.clearRect(0,0,w,h);

            float freeMemory  = Runtime.getRuntime().freeMemory();
            float totalMemory = Runtime.getRuntime().totalMemory();
            //float totalMemory = Runtime.getRuntime().maxMemory();

            // .. Draw allocated and used strings ..
            big.setColor(Color.green);
            big.drawString(String.valueOf((int) totalMemory/1024) + "Ko allocated",  4.0f, ascent+0.5f);
            usedStr = String.valueOf(((int) (totalMemory - freeMemory))/1024) + "Ko used";
            big.drawString(usedStr, 4, h-descent);

            // Calculate remaining size
            float ssH = ascent + descent;
            float remainingHeight = (h - (ssH*2) - 0.5f);
            int nbBlocks = 20;
            float blockHeight = remainingHeight/nbBlocks;
            float blockWidth = 20.0f;
            //float remainingWidth = (w - blockWidth - 10);

            // .. Memory Free ..
            big.setColor(mfColor);
            int MemUsage = (int) ((freeMemory / totalMemory) * nbBlocks);
            int i = 0;
            for ( ; i < MemUsage ; i++) {
                mfRect.setRect(5,  ssH+i*blockHeight, blockWidth, blockHeight-1);
                big.fill(mfRect);
            }

            // .. Memory Used ..
            big.setColor(Color.green);
            for ( ; i < nbBlocks; i++)  {
                muRect.setRect(5, ssH+i*blockHeight, blockWidth, blockHeight-1);
                big.fill(muRect);
            }
            
            // .. Draw History Lines ..
            big.setColor(graphColor);
            int graphX = 30;
            int graphY = (int) ssH;
            int graphW = w - graphX - 5;
            int graphH = (int) remainingHeight;
            for (i = 0 ; i < nbBlocks; i++)  {
                muRect.setRect(graphX, graphY+i*blockHeight-0.5f, graphW, blockHeight);
                big.draw(muRect);
            }

            // .. Draw History columns ..
            int graphColumn = graphW/15;

            if (columnInc == 0) {
                columnInc = graphColumn;
            }
            for (int j = graphX+columnInc; j < graphW+graphX; j+=graphColumn) {
                graphLine.setLine(j,graphY,j,graphY+graphH);
                big.draw(graphLine);
            }

            --columnInc;

            if (pts == null) {
                pts = new int[graphW];
                ptNum = 0;
            } else if (pts.length != graphW) {
                int tmp[] = null;
                if (ptNum < graphW) {     
                    tmp = new int[ptNum];
                    System.arraycopy(pts, 0, tmp, 0, tmp.length);
                } else {        
                    tmp = new int[graphW];
                    System.arraycopy(pts, pts.length-tmp.length, tmp, 0, tmp.length);
                    ptNum = tmp.length - 2;
                }
                pts = new int[graphW];
                System.arraycopy(tmp, 0, pts, 0, tmp.length);
            } else {
                big.setColor(Color.yellow);
                pts[ptNum] = (int)(graphY+graphH*(freeMemory/totalMemory));
                for (int j=graphX+graphW-ptNum, k=0;k < ptNum; k++, j++) {
                    if (k != 0) {
                        if (pts[k] != pts[k-1]) {
                            big.drawLine(j-1, pts[k-1], j, pts[k]);
                        } else {
                            big.fillRect(j, pts[k], 1, 1);
                        }
                    }
                }
                if (ptNum+2 == pts.length) {
                    // throw out oldest point
                    for (int j = 1;j < ptNum; j++) {
                        pts[j-1] = pts[j];
                    }
                    --ptNum;
                } else {
                    ptNum++;
                }
            }
            g.drawImage(bimg, 0, 0, this);
        }


        public void start() {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("MemoryMonitor");
            thread.start();
        }


        public synchronized void stop() {
            thread = null;
            notify();
        }


        public void run() {

            Thread me = Thread.currentThread();

            while (thread == me && !isShowing() || getSize().width == 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) { return; }
            }
    
            while (thread == me && isShowing()) {
                Dimension d = getSize();
                if (d.width != w || d.height != h) {
                    w = d.width;
                    h = d.height;
                    bimg = (BufferedImage) createImage(w, h);
                    big = bimg.createGraphics();
                    big.setFont(font);
                    FontMetrics fm = big.getFontMetrics(font);
                    ascent = fm.getAscent();
                    descent = fm.getDescent();
                }
                repaint();
                try {
                    Thread.sleep(sleepAmount);
                } catch (InterruptedException e) { break; }
                if (JMemoryMonitor.dateStampCB.isSelected()) {
                     System.out.println(new Date().toString() + " " + usedStr);
                }
            }
            thread = null;
        }
    }


    public static void main(String s[]) {
        final JMemoryMonitor demo = new JMemoryMonitor();
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
            public void windowDeiconified(WindowEvent e) { demo.surf.start(); }
            public void windowIconified(WindowEvent e) { demo.surf.stop(); }
        };
        JFrame f = new JFrame("Memory Monitor");
        f.addWindowListener(l);
        f.getContentPane().add("Center", demo);
        f.pack();
        f.setSize(new Dimension(200,200));
        f.setVisible(true);
        demo.surf.start();
    }
}