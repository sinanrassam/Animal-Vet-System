/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package AnimalVet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author sinan
 */
public class AnimalProcessor {
    
    private ArrayHeap<AnimalPatient> waitList;
    
    private static final String JAXP_SCHEMA_LANGUAGE
            = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA
            = "http://www.w3.org/2001/XMLSchema";
    
    public AnimalProcessor() {
        waitList = new ArrayHeap<>();
    }
    
    public void addAnimal(AnimalPatient animal) {
        waitList.add(animal);
    }
    
    public AnimalPatient getNextAnimal() {
        if (!waitList.isEmpty()) {
            AnimalPatient animal = waitList.getMin();
            animal.updateDate(new Date());
            return animal;
        } else {
            return null;
        }
    }
    
    public AnimalPatient releaseAnimal() {
        return waitList.removeMin();
    }
    
    public int animalsLeftToProcess() {
        return waitList.size() - 1;
    }
    
    public void loadAnimalsFromXML(Document document) throws ParseException {
        DOMUtilities domUtil = new DOMUtilities();
        
        AnimalPatient animal;
        String species, name, priority, picURL, symptoms, treatment, dateSeen;
        Date dateLastSeen;
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        Collection<Node> mainNodes = domUtil.getAllChildNodes(document, "animals");
        for (Node node : mainNodes) {
            Node description = domUtil.getAllChildNodes(node, "description").iterator().next();
            String desc = domUtil.getTextContent(description);
            System.out.println("Loading " + desc + "...");
            
            Collection<Node> animalNodes = domUtil.getAllChildNodes(node, "animal");
            
            if (animalNodes.iterator().hasNext()) {
                for (Node animalNode : animalNodes) {
                    species = domUtil.getAttributeString(animalNode, "species");
                    name = domUtil.getAttributeString(animalNode, "name");
                    priority = domUtil.getAttributeString(animalNode, "priority");
                    picURL = getXmlText(domUtil, animalNode, "picURL");
                    symptoms = getXmlText(domUtil, animalNode, "symptoms");
                    treatment = getXmlText(domUtil, animalNode, "treatment");
                    dateSeen = getXmlText(domUtil, animalNode, "dateSeen");
                    
                    dateLastSeen = dateFormat.parse(dateSeen);
                    
                    animal = new AnimalPatient(species, name, dateLastSeen);
                    animal.setPriority(Integer.parseInt(priority));
                    animal.loadImage(picURL);
                    animal.setSymptoms(symptoms);
                    animal.setTreatment(treatment);
                    addAnimal(animal);
                }
                System.out.println(desc + "Successfully Loaded!");
            }
        }
    }
    
    private String getXmlText(DOMUtilities domUtil, Node node, String s) {
        boolean hasNext = domUtil.getAllChildNodes(node, s).iterator().hasNext();
        return (hasNext) ? domUtil.getTextContent(domUtil.getAllChildNodes(node, s).iterator().next()) : null;
    }
    
    public void saveAnimalsToXML(String location) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        document.appendChild(document.createComment("document of a list of animals in a vet"));
        // root element
        Element root = document.createElement("animals");
        document.appendChild(root);
        
        Attr attr = document.createAttribute("xmlns:xsi");
        attr.setValue("http://www.w3.org/2001/XMLSchema-instance");
        root.setAttributeNode(attr);
        
        attr = document.createAttribute("xsi:noNamespaceSchemaLocation");
        attr.setValue("animals.xsd");
        root.setAttributeNode(attr);
        
        Element element = document.createElement("description");
        element.appendChild(document.createTextNode(new Date() + " Animals"));
        root.appendChild(element);
        
        ArrayHeap<AnimalPatient> newList = new ArrayHeap<>();
        AnimalPatient animal;
        Element al;
        String text;
        while (!waitList.isEmpty()) {
            animal = waitList.removeMin();
            
            al = document.createElement("animal");
            root.appendChild(al);
            
            attr = document.createAttribute("name");
            attr.setValue(animal.getName());
            al.setAttributeNode(attr);
            
            attr = document.createAttribute("species");
            attr.setValue(animal.getSpecies());
            al.setAttributeNode(attr);
            
            attr = document.createAttribute("priority");
            attr.setValue(Integer.toString(animal.getPriority()));
            al.setAttributeNode(attr);
            
            text = animal.getImageUrl();
            if ((text != null) && (!text.equals(""))) {
                element = document.createElement("picURL");
                element.appendChild(document.createTextNode(text));
                al.appendChild(element);
            }
            
            text = animal.getSymptoms();
            if ((text != null) && (!text.equals("unknown"))) {
                element = document.createElement("symptoms");
                element.appendChild(document.createTextNode(text));
                al.appendChild(element);
            }
            
            text = animal.getTreatment();
            if (text != null) {
                element = document.createElement("treatment");
                element.appendChild(document.createTextNode(text));
                al.appendChild(element);
            }
            
            element = document.createElement("dateSeen");
            element.appendChild(document.createTextNode(animal.getDateLastSeen()));
            al.appendChild(element);
            
            
            newList.add(animal);
        }
        waitList = newList;
        
        // create the xml file
        //transform the DOM Object to an XML File
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(location));
        
        // If you use
        // StreamResult result = new StreamResult(System.out);
        // the output will be pushed to the standard output ...
        // You can use that for debugging
        transformer.transform(domSource, streamResult);
        
        System.out.println("Saved");
    }
    
    @Override
    public String toString() {
        return waitList.toString();
    }
    
    public static void main(String[] args) throws ParseException, ParserConfigurationException, SAXException, IOException {
        AnimalProcessor animals = new AnimalProcessor();
        AnimalPatient a;
        Document document;
        
// create a validating DOM document builder
// using the default parser
DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
builderFactory.setNamespaceAware(true);
builderFactory.setValidating(true);
builderFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
DocumentBuilder builder;
builder = builderFactory.newDocumentBuilder();
// parse the input stream
document = builder.parse(new FileInputStream(new File("AnimalsInVet.xml")));
document.getDocumentElement().normalize();

animals.loadAnimalsFromXML(document);

SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
System.out.println("Animals :" + animals);

System.out.println("***********Get next: " + animals.getNextAnimal() + "***********");

System.out.println("Animals :" + animals);

a = animals.releaseAnimal();
System.out.println("***********Get next: " + animals.getNextAnimal() + "***********");
animals.addAnimal(a);

System.out.println("Animals :" + animals);

a = animals.releaseAnimal();
System.out.println("***********Get next: " + animals.getNextAnimal() + "***********");
animals.addAnimal(a);

System.out.println("Animals :" + animals);
a = animals.releaseAnimal();
System.out.println("***********Get next: " + animals.getNextAnimal() + "***********");
animals.addAnimal(a);

System.out.println("Animals :" + animals);
a = animals.releaseAnimal();
System.out.println("***********Get next: " + animals.getNextAnimal() + "***********");
animals.addAnimal(a);

System.out.println("Animals :" + animals);

    }
}
