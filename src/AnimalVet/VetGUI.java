/*
 * A GUI which keeps a visual representation of the AnimalProcessor. It displays
 * the AnimalPatient at the front of the wait list
 */
package AnimalVet;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author sinan
 */
public class VetGUI extends JPanel implements ActionListener {
    
    private JLabel counterLabel;
    private static AnimalPatient currentAnimal;
    private static AnimalProcessor animals;
    private static JPanel currentAnimalPanel;
    private JButton newPatient, seeLater, release, loadXML, saveXML, updatePic;
    
    private final String JAXP_SCHEMA_LANGUAGE
            = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private final String W3C_XML_SCHEMA
            = "http://www.w3.org/2001/XMLSchema";
    private final String counterText = "Animals still waiting to be seen: ";
    private final String notAvailableText = "No more animals waiting to be seen!";
    
    public VetGUI() {
        super(new BorderLayout());
        counterLabel = new JLabel(counterText + animals.animalsLeftToProcess(), SwingConstants.CENTER);
        counterLabel.setFont(new Font("Comic Sans", Font.BOLD, 14));
        add(counterLabel, BorderLayout.NORTH);
        
        add(currentAnimalPanel, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel();
        newPatient = new JButton("New patient");
        newPatient.addActionListener(this);
        southPanel.add(newPatient);
        seeLater = new JButton("See Later");
        seeLater.addActionListener(this);
        southPanel.add(seeLater);
        release = new JButton("Release");
        release.addActionListener(this);
        southPanel.add(release);
        loadXML = new JButton("Load XML");
        loadXML.addActionListener(this);
        southPanel.add(loadXML);
        saveXML = new JButton("Save XML");
        saveXML.addActionListener(this);
        southPanel.add(saveXML);
        updatePic = new JButton("Update Pic");
        updatePic.addActionListener(this);
        southPanel.add(updatePic);
        
        super.add(southPanel, BorderLayout.SOUTH);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String input = null;
        if (source == newPatient) {
            JTextField species = new JTextField(10);
            JTextField name = new JTextField(10);
            JPanel myPanel = new JPanel();
            myPanel.add(new JLabel("Species:"));
            myPanel.add(species);
            myPanel.add(Box.createHorizontalStrut(15)); // a spacer
            myPanel.add(new JLabel("Name:"));
            myPanel.add(name);
            
            int result = JOptionPane.showConfirmDialog(null, myPanel,
                    "Input species and name", JOptionPane.OK_CANCEL_OPTION);
            boolean success = ((result == JOptionPane.OK_OPTION) && (species.getText() != null)
                    && !("").equals(species.getText()) && (name.getText() != null)
                    && !("").equals(name.getText()));
            if (success) {
                animals.addAnimal(new AnimalPatient(species.getText(), name.getText()));
                updatePanel();
                counterLabel.setText(counterText + getAnimalsWaiting());
            }
        } else if ((source == seeLater) && (animals.animalsLeftToProcess() > 0)) {
            AnimalPatient animal = animals.releaseAnimal();
            updatePanel();
            animals.addAnimal(animal);
        } else if ((source == release) && (animals.animalsLeftToProcess() + 1 > 0)) {
            animals.releaseAnimal();
            updatePanel();
            counterLabel.setText(counterText + getAnimalsWaiting());
        } else if (source == loadXML) {
            input = openFile("XML Files", "xml");
            if ((input != null) && !input.equals("")) {
                try {
                    // create a validating DOM document builder
                    // using the default parser
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    builderFactory.setNamespaceAware(true);
                    builderFactory.setValidating(true);
                    builderFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
                    DocumentBuilder builder;
                    builder = builderFactory.newDocumentBuilder();
                    // parse the input stream
                    Document document = builder.parse(new FileInputStream(new File(input)));
                    document.getDocumentElement().normalize();
                    animals.loadAnimalsFromXML(document);
                } catch (ParserConfigurationException | SAXException | IOException | ParseException ex) {
                    System.out.println("Error: " + ex);
                }
                counterLabel.setText(counterText + getAnimalsWaiting());
                updatePanel();
            }
        } else if ((source == saveXML) && (currentAnimal != null)) {
            input = saveFile("XML Files", "xml");
            if ((input != null) && !input.equals("")) {
                try {
                    animals.saveAnimalsToXML(input);
                } catch (ParserConfigurationException | TransformerException ex) {
                    System.out.println("Error: " + ex);
                }
            }
        } else if ((source == updatePic) && (currentAnimal != null)) {
            input = openFile("JPG Images", "jpg");
            if ((input != null) && !input.equals("")) {
                currentAnimal.loadImage(input);
            }
        }
    }
    
    private void updatePanel() {
        currentAnimal = animals.getNextAnimal();
        super.remove(currentAnimalPanel);
        if (currentAnimal != null) {
            currentAnimalPanel = currentAnimal.getDisplayPanel();
            super.add(currentAnimalPanel, BorderLayout.CENTER);
        } else {
            System.out.println(notAvailableText);
        }
        super.revalidate();
        super.repaint();
    }
    
    private int getAnimalsWaiting() {
        return (animals.animalsLeftToProcess() < 0) ? 0 : animals.animalsLeftToProcess();
    }
    
    private String openFile(String desc, String ext) {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter(desc, ext));
        int status = chooser.showOpenDialog(chooser);
        return (status != JFileChooser.APPROVE_OPTION) ? null
                : chooser.getSelectedFile().getPath();
    }
    
    private String saveFile(String desc, String ext) {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter(desc, ext));
        int status = chooser.showSaveDialog(this);
        return (status != JFileChooser.APPROVE_OPTION) ? null
                : chooser.getSelectedFile().getPath();
    }
    
    public static void main(String[] args) {
        animals = new AnimalProcessor();
        animals.addAnimal(new AnimalPatient("Dog", "Bob"));
        currentAnimal = animals.getNextAnimal();
        currentAnimalPanel = currentAnimal.getDisplayPanel();
        
        JFrame frame = new JFrame("Vet of cute Animals");
// kill all threads when frame closes
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
frame.getContentPane().add(new VetGUI());
frame.pack();
// position the frame in the middle of the screen
Toolkit tk = Toolkit.getDefaultToolkit();
Dimension screenDimension = tk.getScreenSize();
Dimension frameDimension = frame.getSize();
frame.setLocation((screenDimension.width - frameDimension.width) / 2,
        (screenDimension.height - frameDimension.height) / 2);
frame.setVisible(true);
// now display something while the main thread is still alive
    }
    
}
