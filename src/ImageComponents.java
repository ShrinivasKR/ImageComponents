/*
 * ImageComponents.java
 * Starter code for A4 // Change this line to "A4 Solution by " + Shrinivas Kopparam Ramanath and UWNetID: shrini.
 * Allows image to be edited different ways using various tools.
 * Also applies the UNION-FIND technique to compress and recolor the images.
 * Also Performs Extra Credit a which adds an option to do the segmentation on the basis of a
 * given DELTA value rather than the nregions value
 * 
 * CSE 373, University of Washington, Autumn 2014.
 * 
 * Starter Code for CSE 373 Assignment 4, Part II.    Starter Code Version 0.3.
 * S. Tanimoto,  Nov. 6, 2014, after feedback from TA Johnson Goh.
 * Minor changes from v0.2: removed blockSize and hashFunctionChoice variables needed in A3,
 *   added a new variable parentID to standardize the way solutions look for A4,
 *   and added stubs for the find and union methods that need to be implemented.
 * 
 */ 

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImageComponents extends JFrame implements ActionListener {
    public static ImageComponents appInstance; // Used in main().

    String startingImage = "donut2.png";
    BufferedImage biTemp, biWorking, biFiltered; // These hold arrays of pixels.
    Graphics gOrig, gWorking; // Used to access the drawImage method.
    int w; // width of the current image.
    int h; // height of the current image.

    int[][] parentID; // For your forest of up-trees.

    /**
     * Finds the root of the given pixelID
     * @param pixelID of a pixel in the image
     * @return the pixelID of the parent
     */
    int find(int pixelID) {
        while(parentID[getYcoord(pixelID)][getXcoord(pixelID)] != -1) {
            pixelID = parentID[getYcoord(pixelID)][getXcoord(pixelID)];
        }
        return pixelID;
    }// Part of your UNION-FIND implementation. You need to complete the implementation of this.

    /**
     * Unions the two pixels with the smaller pixel id as the parent
     * @param pixelID1 one pixel
     * @param pixelID2 another pixel
     */
    void union(int pixelID1, int pixelID2) {
        pixelID1 = find(pixelID1); //finds the root of the first pixel
        pixelID2 = find(pixelID2); //finds the root of the second pixel
        int x1 = getXcoord(pixelID1);
        int x2 = getXcoord(pixelID2);
        int y1 = getYcoord(pixelID1);
        int y2 = getYcoord(pixelID2);
        if(pixelID1 < pixelID2) { //if the first pixel's root is lower than the second's
            parentID[y2][x2] = pixelID1; // make the first pixel the parent of the second pixel
        } else {
            parentID[y1][x1] = pixelID2; // make the second pixel the parent of the first pixel
        }
    }  // Another part of your UNION-FIND implementation.  Also complete this one.

    /**
     * Returns the x coordinate of a pixel
     * @param pixelID
     * @return x coordinate
     */
    private int getXcoord(int pixelID) {
        return pixelID % w;
    }

    /**
     * Returns the y coordinate of a pixel
     * @param pixelID
     * @return y coordinate
     */
    private int getYcoord(int pixelID) {
        return pixelID / w;
    }

    JPanel viewPanel; // Where the image will be painted.
    JPopupMenu popup;
    JMenuBar menuBar;
    JMenu fileMenu, imageOpMenu, ccMenu, helpMenu;
    JMenuItem loadImageItem, saveAsItem, exitItem;
    JMenuItem lowPassItem, highPassItem, photoNegItem, RGBThreshItem;

    /**
     *
     */
    JMenuItem CCItem1;
    JMenuItem CCItem2;
    JMenuItem CCItem3;
    JMenuItem aboutItem, helpItem;
    
    JFileChooser fileChooser; // For loading and saving images.

    /**
     * Color which stores r,g,b values of a color
     */
    public class Color {
        int r, g, b;

        Color(int r, int g, int b) {
            this.r = r; this.g = g; this.b = b;    		
        }

        /**
         * Calculates the euclidian distance between the current pixel and another
         * @param c2 other pixel
         * @return Euclidian distance between the two pixels
         */
        int euclideanDistance(Color c2) {
            int differenceRed = r - c2.r;   //distance between the red
            int differenceGreen = g - c2.g; //distance between the green
            int differenceBlue = b - c2.b;  //distance between the blue
            return (differenceRed * differenceRed  + differenceGreen * differenceGreen + differenceBlue * differenceBlue);
        }
    }

    /**
     * Edge between a pair of adjacent pixels, also carries a weight
     */
    public class Edge implements Comparable<Edge> {
        /**
         * Weight, and the two endpoints of the edge
         */
        private int endpoint0, endpoint1, weight;

        // Creates a new edge with given endpoints and weight
        public Edge(int endpoint0, int endpoint1) {
            this.endpoint0 = endpoint0;
            this.endpoint1 = endpoint1;
            int currentPixelRGB = biWorking.getRGB(getXcoord(endpoint0), getYcoord(endpoint0)); //RGB of current pixel
            Color currentPixelColor = new Color(currentPixelRGB >> 16 & 255, currentPixelRGB >> 8 & 255, currentPixelRGB & 255); //Determines color of current pixel
            int neighborPixelRGB = biWorking.getRGB(getXcoord(endpoint1), getYcoord(endpoint1)); //RBG of neighbor pixel
            Color neighborPixelColor = new Color(neighborPixelRGB >> 16 & 255, neighborPixelRGB >> 8 & 255, neighborPixelRGB & 255); //Determines color of neighboring pixel
            double colorDistance = currentPixelColor.euclideanDistance(neighborPixelColor); //Distance between two pixels
            this.weight = (int) (colorDistance * colorDistance); //The color distance squared
        }

        /**
         * To find the endpoint0
         * @return endpoint0
         */
        public int getEndPoint0() {
            return endpoint0;
        }

        /**
         * To find the endpoint1
         * @return endpoint1
         */
        public int getEndpoint1() {
            return endpoint1;
        }

        /**
         * To get the color distance between pixels
         * @return weight of distance between pixels
         */
        public  int getWeight() {
            return weight;
        }

        /**
         * Allows for the comparison of two edges based on their weight
         * @param e2 another edge
         * @return whether this edge's weight is equal to, greater than, or lesser than the other edge's weight
         */
        public int compareTo(Edge e2) {
            if((this.weight < e2.weight)) {
            return -1;
            } else if(this.weight == e2.weight) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    // Some image manipulation data definitions that won't change...
    static LookupOp PHOTONEG_OP, RGBTHRESH_OP;
    static ConvolveOp LOWPASS_OP, HIGHPASS_OP;
    
    public static final float[] SHARPENING_KERNEL = { // sharpening filter kernel
        0.f, -1.f,  0.f,
       -1.f,  5.f, -1.f,
        0.f, -1.f,  0.f
    };

    public static final float[] BLURRING_KERNEL = {
        0.1f, 0.1f, 0.1f,    // low-pass filter kernel
        0.1f, 0.2f, 0.1f,
        0.1f, 0.1f, 0.1f
    };
    
    public ImageComponents() { // Constructor for the application.
        setTitle("Image Analyzer"); 
        addWindowListener(new WindowAdapter() { // Handle any window close-box clicks.
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });

        // Create the panel for showing the current image, and override its
        // default paint method to call our paintPanel method to draw the image.
        viewPanel = new JPanel(){public void paint(Graphics g) { paintPanel(g);}};
        add("Center", viewPanel); // Put it smack dab in the middle of the JFrame.

        // Create standard menu bar
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        fileMenu = new JMenu("File");
        imageOpMenu = new JMenu("Image Operations");
        ccMenu = new JMenu("Connected Components");
        helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        menuBar.add(imageOpMenu);
        menuBar.add(ccMenu);
        menuBar.add(helpMenu);

        // Create the File menu's menu items.
        loadImageItem = new JMenuItem("Load image...");
        loadImageItem.addActionListener(this);
        fileMenu.add(loadImageItem);
        saveAsItem = new JMenuItem("Save as full-color PNG");
        saveAsItem.addActionListener(this);
        fileMenu.add(saveAsItem);
        exitItem = new JMenuItem("Quit");
        exitItem.addActionListener(this);
        fileMenu.add(exitItem);

        // Create the Image Operation menu items.
        lowPassItem = new JMenuItem("Convolve with blurring kernel");
        lowPassItem.addActionListener(this);
        imageOpMenu.add(lowPassItem);
        highPassItem = new JMenuItem("Convolve with sharpening kernel");
        highPassItem.addActionListener(this);
        imageOpMenu.add(highPassItem);
        photoNegItem = new JMenuItem("Photonegative");
        photoNegItem.addActionListener(this);
        imageOpMenu.add(photoNegItem);
        RGBThreshItem = new JMenuItem("RGB Thresholds at 128");
        RGBThreshItem.addActionListener(this);
        imageOpMenu.add(RGBThreshItem);

 
        // Create CC menu stuff.
        CCItem1 = new JMenuItem("Compute Connected Components and Recolor");
        CCItem1.addActionListener(this);
        ccMenu.add(CCItem1);
        CCItem2 = new JMenuItem("Segment Image and Recolor using nregions");
        CCItem2.addActionListener(this);
        ccMenu.add(CCItem2);
        CCItem3 = new JMenuItem("Segment Image Using a Maximum Pixel DELTA ");
        CCItem3.addActionListener(this);
        ccMenu.add(CCItem3);

        
        // Create the Help menu's item.
        aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(this);
        helpMenu.add(aboutItem);
        helpItem = new JMenuItem("Help");
        helpItem.addActionListener(this);
        helpMenu.add(helpItem);

        // Initialize the image operators, if this is the first call to the constructor:
        if (PHOTONEG_OP==null) {
            byte[] lut = new byte[256];
            for (int j=0; j<256; j++) {
                lut[j] = (byte)(256-j); 
            }
            ByteLookupTable blut = new ByteLookupTable(0, lut); 
            PHOTONEG_OP = new LookupOp(blut, null);
        }
        if (RGBTHRESH_OP==null) {
            byte[] lut = new byte[256];
            for (int j=0; j<256; j++) {
                lut[j] = (byte)(j < 128 ? 0: 200);
            }
            ByteLookupTable blut = new ByteLookupTable(0, lut); 
            RGBTHRESH_OP = new LookupOp(blut, null);
        }
        if (LOWPASS_OP==null) {
            float[] data = BLURRING_KERNEL;
            LOWPASS_OP = new ConvolveOp(new Kernel(3, 3, data),
                                        ConvolveOp.EDGE_NO_OP,
                                        null);
        }
        if (HIGHPASS_OP==null) {
            float[] data = SHARPENING_KERNEL;
            HIGHPASS_OP = new ConvolveOp(new Kernel(3, 3, data),
                                        ConvolveOp.EDGE_NO_OP,
                                        null);
        }
        loadImage(startingImage); // Read in the pre-selected starting image.
        setVisible(true); // Display it.
    }
    
    /*
     * Given a path to a file on the file system, try to load in the file
     * as an image.  If that works, replace any current image by the new one.
     * Re-make the biFiltered buffered image, too, because its size probably
     * needs to be different to match that of the new image.
     * Also makes each pixel a root
     */
    public void loadImage(String filename) {
        try {
            biTemp = ImageIO.read(new File(filename));
            w = biTemp.getWidth();
            h = biTemp.getHeight();
            viewPanel.setSize(w,h);
            biWorking = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gWorking = biWorking.getGraphics();
            gWorking.drawImage(biTemp, 0, 0, null);
            biFiltered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            pack(); // Lay out the JFrame and set its size.
            repaint();
        } catch (IOException e) {
            System.out.println("Image could not be read: "+filename);
            System.exit(1);
        }
    }

    /**
     * Assign each pixel in the image before the unions occur
     */
    private void initializeParentIDs() {
        parentID = new int[h][w];
        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                parentID[y][x] = -1;
            }
        }
    }

    /* Menu handlers
     */
    void handleFileMenu(JMenuItem mi){
        System.out.println("A file menu item was selected.");
        if (mi==loadImageItem) {
            File loadFile = new File("image-to-load.png");
            if (fileChooser==null) {
                fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(loadFile);
                fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", new String[] { "JPG", "JPEG", "GIF", "PNG" }));
            }
            int rval = fileChooser.showOpenDialog(this);
            if (rval == JFileChooser.APPROVE_OPTION) {
                loadFile = fileChooser.getSelectedFile();
                loadImage(loadFile.getPath());
            }
        }
        if (mi==saveAsItem) {
            File saveFile = new File("savedimage.png");
            fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(saveFile);
            int rval = fileChooser.showSaveDialog(this);
            if (rval == JFileChooser.APPROVE_OPTION) {
                saveFile = fileChooser.getSelectedFile();
                // Save the current image in PNG format, to a file.
                try {
                    ImageIO.write(biWorking, "png", saveFile);
                } catch (IOException ex) {
                    System.out.println("There was some problem saving the image.");
                }
            }
        }
        if (mi==exitItem) { this.setVisible(false); System.exit(0); }
    }

    void handleEditMenu(JMenuItem mi){
        System.out.println("An edit menu item was selected.");
    }

    void handleImageOpMenu(JMenuItem mi){
        System.out.println("An imageOp menu item was selected.");
        if (mi==lowPassItem) { applyOp(LOWPASS_OP); }
        else if (mi==highPassItem) { applyOp(HIGHPASS_OP); }
        else if (mi==photoNegItem) { applyOp(PHOTONEG_OP); }
        else if (mi==RGBThreshItem) { applyOp(RGBTHRESH_OP); }
        repaint();
    }

    void handleCCMenu(JMenuItem mi) {
        System.out.println("A connected components menu item was selected.");
        if (mi==CCItem1) { computeConnectedComponents(); }
        if (mi==CCItem2) {
            int nregions = 25; // default value.
            String inputValue = JOptionPane.showInputDialog("Please input the number of regions desired");
            try {
                nregions = (new Integer(inputValue)).intValue();
            }
            catch(Exception e) {
                System.out.println(e);
                System.out.println("That did not convert to an integer. Using the default: 25.");
            }
            System.out.println("nregions is "+nregions);
            // Call your image segmentation method here.
            segmentImage(0, nregions);

        }
        if (mi==CCItem3) {
            int delta = 12; // default value.
            String inputValue = JOptionPane.showInputDialog("Please input the delta desired for segmenting");
            try {
                delta = (new Integer(inputValue)).intValue();
            }
            catch(Exception e) {
                System.out.println(e);
                System.out.println("That did not convert to an integer. Using the default: 12.");
            }
            System.out.println("delta is "+ delta);
            // Call your image segmentation method here.
            segmentImage(1, delta);
        }
    }
    void handleHelpMenu(JMenuItem mi){
        System.out.println("A help menu item was selected.");
        if (mi==aboutItem) {
            System.out.println("About: Well this is my program.");
            JOptionPane.showMessageDialog(this,
                "Image Components, Starter-Code Version.",
                "About",
                JOptionPane.PLAIN_MESSAGE);
        }
        else if (mi==helpItem) {
            System.out.println("In case of panic attack, select File: Quit.");
            JOptionPane.showMessageDialog(this,
                "To load a new image, choose File: Load image...\nFor anything else, just try different things.",
                "Help",
                JOptionPane.PLAIN_MESSAGE);
        }
    }

    /*
     * Used by Swing to set the size of the JFrame when pack() is called.
     */
    public Dimension getPreferredSize() {
        return new Dimension(w, h+50); // Leave some extra height for the menu bar.
    }

    public void paintPanel(Graphics g) {
        g.drawImage(biWorking, 0, 0, null);
    }
            	
    public void applyOp(BufferedImageOp operation) {
        operation.filter(biWorking, biFiltered);
        gWorking.drawImage(biFiltered, 0, 0, null);
    }

    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource(); // What Swing object issued the event?
        if (obj instanceof JMenuItem) { // Was it a menu item?
            JMenuItem mi = (JMenuItem)obj; // Yes, cast it.
            JPopupMenu pum = (JPopupMenu)mi.getParent(); // Get the object it's a child of.
            JMenu m = (JMenu) pum.getInvoker(); // Get the menu from that (popup menu) object.
            //System.out.println("Selected from the menu: "+m.getText()); // Printing this is a debugging aid.

            if (m==fileMenu)    { handleFileMenu(mi);    return; }  // Handle the item depending on what menu it's from.
            if (m==imageOpMenu) { handleImageOpMenu(mi); return; }
            if (m==ccMenu)      { handleCCMenu(mi);      return; }
            if (m==helpMenu)    { handleHelpMenu(mi);    return; }
        } else {
            System.out.println("Unhandled ActionEvent: "+e.getActionCommand());
        }
    }


    // Use this to put color information into a pixel of a BufferedImage object.
    void putPixel(BufferedImage bi, int x, int y, int r, int g, int b) {
        int rgb = (r << 16) | (g << 8) | b; // pack 3 bytes into a word.
        bi.setRGB(x,  y, rgb);
    }

    /**
     * Computes relationships between pixels and performs recoloration
     * Also keeps track of the number of unions performed
     */
    void computeConnectedComponents() {
        initializeParentIDs();
        int count = 0;

        // Connects components if they are the same color and their roots
        // are not the same
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) { // joins edges between pixels based on Instructor specifications
                int currentPixelID = (y) * w + x;
                int neighboringPixelID = y * w + x + 1;
                if (x < w - 1 && find(currentPixelID) != find(neighboringPixelID) //compares to neighboring pixel to the right
                        && biWorking.getRGB(x, y) == biWorking.getRGB(x + 1, y)) { //unions if the pixel exists and parents aren't same but colors are
                    union(currentPixelID, neighboringPixelID);
                    count++;
                }
                neighboringPixelID = (y + 1) * w + x;
                if (y < h - 1 && find(currentPixelID) != find(neighboringPixelID) //does the same as the above except compares to the pixel below
                        && biWorking.getRGB(x, y) == biWorking.getRGB(x, y + 1)) {
                    union(currentPixelID, neighboringPixelID);
                    count++;
                }
            }
        }
        System.out.println("The number of times that the method UNION was called for this image is: " + count + ".");
        recolorImage();
    }

    /**
     * Creates various regions of segments of colors
     * @param segmentType the method we want to use to segment the image
     * @param givenValue the number of segments or the delta value
     */
    private void segmentImage(int segmentType, int givenValue) {
        initializeParentIDs();  //Sets the uptree up
        PriorityQueue<Edge> edges = new PriorityQueue<Edge>();
        //Creates the weighted pixel graph
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int currPixelID = (y) * w + x;
                if (x < w - 1 ) {
                    int neighborPixelID = y * w + x + 1;
                    edges.add(new Edge(currPixelID, neighborPixelID));
                }
                if (y < h - 1) {
                    int neighborPixelID = (y + 1) * w + x;
                    edges.add(new Edge(currPixelID, neighborPixelID));
                }
            }
        }
        /**
         * Segments the image either on the number of segments or the delta value given as requested by the user
         */
        switch(segmentType) {
            /**
             * Segments based on given nregions
             */
            case 0:
                int numSegments = w * h;
                //Segment until the number of segments created matches the desired number
                while (numSegments > givenValue) {
                    Edge currentEdge = edges.remove();
                    int pixelID0 = currentEdge.getEndPoint0();
                    int pixelID1 = currentEdge.getEndpoint1();
                    if (find(pixelID0) != find(pixelID1)) {
                        union(pixelID0, pixelID1);
                        numSegments--;
                    }
                }
                break;
            /**
             * Segments based on max DELTA value
             */
            case 1:
                if (edges.size() > 0) {
                    Edge currentEdge = edges.remove();
                    //Segments until the weight of the edge is bigger than given delta
                    //or no more edges exist
                    while (currentEdge != null && currentEdge.weight <= givenValue) {
                        int pixelID0 = currentEdge.getEndPoint0();
                        int pixelID1 = currentEdge.getEndpoint1();
                        if (find(pixelID0) != find(pixelID1)) {
                            union(pixelID0, pixelID1);
                        }
                        currentEdge = (edges.size() > 0) ? edges.remove() : null;
                    }
                }
                break;
        }
        recolorImage();
    }

    /**
     * Recolors the image
     */
    private void recolorImage() {
        int count = 0;
        HashMap<Integer, Integer> componentNumber = new HashMap<Integer, Integer>();
        for (int y = 0; y < h; y++) { //Counts the total number of roots
            for (int x = 0; x < w; x++) {
                if (parentID[y][x] == -1) {
                    componentNumber.put(y * w + x, count);
                    count++;
                }
            }
        }
        System.out.println("The number of connected components in this image is: " + count + ".");
        ProgressiveColors progressiveColors = new ProgressiveColors();
        for (int y = 0; y < h; y++) { //Recolors the image based on the current pixel's root.
            for (int x = 0; x < w; x++) {
                Integer rootID = find(y * w + x);
                int[] rgb = progressiveColors.progressiveColor(componentNumber.get(rootID));
                putPixel(biWorking, x, y, rgb[0], rgb[1], rgb[2]);
            }
        }
        repaint();
    }

    /* This main method can be used to run the application. */
    public static void main(String s[]) {
        appInstance = new ImageComponents();
    }
}
