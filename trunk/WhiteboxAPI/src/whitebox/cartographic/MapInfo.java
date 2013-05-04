/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package whitebox.cartographic;

import java.io.*;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
//import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import whitebox.interfaces.CartographicElement;
import whitebox.structures.BoundingBox;

/**
 * This class is used to manage the layers and properties of maps. The actual 
 * map display is handled by the MapRenderer class.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MapInfo implements java.io.Serializable{
    // Fields.
    private String mapName = "";
    private transient boolean dirty = false; 
    private String fileName = "";
    private boolean pageVisible = true;
    private PageFormat pageFormat = new PageFormat();
    private double margin = 0.0;
    private int numMapAreas = 0;
    private BoundingBox pageBox = new BoundingBox();
    
    private ArrayList<CartographicElement> listOfCartographicElements = new ArrayList<>();
            
    /**
     * MapInfo constructor
     */
    public MapInfo(String mapTitle) {
        try {
            //pathSep = File.separator;
            //applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            //paletteDirectory = applicationDirectory + pathSep + "resources" + pathSep + "palettes" + pathSep;
            //pageFormat = new PageFormat();
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
            Paper paper = pageFormat.getPaper();
            double width = paper.getWidth();
            double height = paper.getHeight();
            double marginInPoints = margin * 72;
            paper.setImageableArea(marginInPoints, marginInPoints, 
                    width - 2 * marginInPoints, height - 2 * marginInPoints);
            pageFormat.setPaper(paper);
            
//            CartographicElement ce = new MapArea("MapArea1");
//            addNewCartographicElement(ce); // the default MapDataView;
            
            
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
    
    public MapInfo() {
        // no-arg constructor
        pageFormat.setOrientation(PageFormat.LANDSCAPE);
        Paper paper = pageFormat.getPaper();
        double width = paper.getWidth();
        double height = paper.getHeight();
        double marginInPoints = margin * 72;
        paper.setImageableArea(marginInPoints, marginInPoints,
                width - 2 * marginInPoints, height - 2 * marginInPoints);
        pageFormat.setPaper(paper);

//        CartographicElement ce = new MapArea("MapArea1");
//        addNewCartographicElement(ce); // the default MapDataView;
            
    }
    
    public final void addNewCartographicElement(CartographicElement ce) {
        ce.setElementNumber(listOfCartographicElements.size());
        listOfCartographicElements.add(ce);
        if (ce instanceof MapArea) {
            numMapAreas++;
            mapAreas.add((MapArea)ce);
            activeMapArea = ce.getElementNumber();
        }
    }
    
    public void removeCartographicElement(int elementNumber) {
        try {
            Collections.sort(listOfCartographicElements);
            listOfCartographicElements.remove(elementNumber);
            // re-order the elements
            Collections.sort(listOfCartographicElements);
            int i = 0;
            for (CartographicElement ce : listOfCartographicElements) {
                ce.setElementNumber(i);
                i++;
            }
            mapAreas.clear();
            for (CartographicElement ce : listOfCartographicElements) {
                if (ce instanceof MapArea) {
                    mapAreas.add((MapArea) ce);
                    activeMapArea = ce.getElementNumber();
                }
            }
            Collections.sort(listOfCartographicElements);
        } catch (Exception e) {
            System.err.println(e.getMessage()); 
        }
    }
    
    public ArrayList<CartographicElement> getCartographicElementList() {
        Collections.sort(listOfCartographicElements);
        return listOfCartographicElements;
    }
    
    public CartographicElement getCartographicElement(int n) {
        if (n >= 0) {
            Collections.sort(listOfCartographicElements);
            return listOfCartographicElements.get(n);
        } else {
            return null;
        }
    }
    
    public void deslectAllCartographicElements() {
        for (CartographicElement ce : listOfCartographicElements) {
            ce.setSelected(false);
        }
        activeMapArea = -1;
    }
    
    public void zoomToPage() {
        pageBox.setMinX(Float.NEGATIVE_INFINITY);
        pageBox.setMinY(Float.NEGATIVE_INFINITY);
        pageBox.setMaxX(Float.NEGATIVE_INFINITY);
        pageBox.setMaxY(Float.NEGATIVE_INFINITY);
    }
    
    public void addMapTitle() {
        // how many map titles are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof MapTitle) { i++; }
        }
        String name = "MapTitle" + (i + 1);
        CartographicElement ce = new MapTitle(getMapName(), name);
        addNewCartographicElement(ce);
    }
    
    public void addMapScale() {
        // how many map scales are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof MapScale) { i++; }
        }
        String name = "MapScale" + (i + 1);
        MapScale ms = new MapScale(name);
        ms.setMapArea(getActiveMapArea());
        addNewCartographicElement((CartographicElement)ms);
    }
    
    public void addNorthArrow() {
        // how many north arrows are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof NorthArrow) { i++; }
        }
        String name = "NorthArrow" + (i + 1);
        CartographicElement ce = new NorthArrow(name);
        addNewCartographicElement(ce);
    }
    
    public void addNeatline() {
        // how many neat lines are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof NeatLine) { i++; }
            ce.setElementNumber(ce.getElementNumber() + 1);
        }
        String name = "Neatline" + (i + 1);
        CartographicElement ce = new NeatLine(name);
        ce.setElementNumber(0); // neatlines are added to the bottom of the list
        listOfCartographicElements.add(ce);
    }
    
    public void addLegend() {
        // how many legends are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof Legend) { i++; }
        }
        String name = "Legend" + (i + 1);
        Legend ce = new Legend(name);
        for (MapArea ma : mapAreas) {
            ce.addMapArea(ma);
        }
        addNewCartographicElement(ce);
    }
    
    private transient ArrayList<MapArea> mapAreas = new ArrayList<MapArea>();
    public void addMapArea() {
        // how many map areas are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof MapArea) { i++; }
        }
        String name = "MapArea" + (i + 1);
        CartographicElement ce = new MapArea(name);
        addNewCartographicElement(ce);
    }
    
    public ArrayList<MapArea> getMapAreas() {
        return mapAreas;
    }
    
    public void promoteMapElement(int index) {
        Collections.sort(listOfCartographicElements);
        if (index < listOfCartographicElements.size() - 1 && index >= 0) {
            listOfCartographicElements.get(index).setElementNumber(index + 1);
            listOfCartographicElements.get(index + 1).setElementNumber(index);
        }
        Collections.sort(listOfCartographicElements);
        activeMapArea = -1;
    }
    
    public void demoteMapElement(int index) {
        Collections.sort(listOfCartographicElements);
        if (index <= listOfCartographicElements.size() - 1 && index > 0) {
            listOfCartographicElements.get(index).setElementNumber(index - 1);
            listOfCartographicElements.get(index - 1).setElementNumber(index);
        }
        Collections.sort(listOfCartographicElements);
        activeMapArea = -1;
    }
    
    public void modifyElement(int elementNumber, CartographicElement ce) {
        Collections.sort(listOfCartographicElements);
        listOfCartographicElements.set(elementNumber, ce);
    }
    
    // Properties
    
    public void setWorkingDirectory(String directory) {
        
    }
    
    public String getMapName() {
        return mapName;
    }
    
    public void setMapName(String title) {
        mapName = title;
        dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public BoundingBox getPageExtent() {
        return pageBox.clone();
    }
    
    public void setPageExtent(BoundingBox extent) {
        pageBox = extent.clone();
    }
    
    public boolean isPageVisible() {
        return pageVisible;
    }

    public void setPageVisible(boolean pageVisible) {
        this.pageVisible = pageVisible;
    }

    public PageFormat getPageFormat() {
        return pageFormat;
    }

    public void setPageFormat(PageFormat pageFormat) {
        this.pageFormat = pageFormat;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }
    
    
    // Methods
    private int activeMapArea = -1;
    
    public int getActiveMapAreaOverlayNumber() {
        if (activeMapArea < 0) {
            return findActiveMapArea();
        } else {
            return activeMapArea;
        }
    }
    
    public MapArea getActiveMapArea() {
        if (activeMapArea < 0) {
            getActiveMapAreaOverlayNumber();
        }
        for (MapArea mapArea : mapAreas) {
            if (mapArea.getElementNumber() == activeMapArea) {
                return mapArea;
            }
        }
        return null;
    }
    
    public void setActiveMapAreaByElementNum(int elementNum) {
        activeMapArea = elementNum;
    }
    
    public MapArea getMapAreaByElementNum(int elementNum) {
        for (MapArea mapArea : mapAreas) {
            if (mapArea.getElementNumber() == elementNum) {
                return mapArea;
            }
        }
        return null;
    }
    
    private int findActiveMapArea() {
        // if there is only one MapArea then return it's element number
        if (numMapAreas == 1) {
            activeMapArea = mapAreas.get(0).getElementNumber();
//            for (CartographicElement ce : listOfCartographicElements) {
//                if (ce instanceof MapArea) {
//                    activeMapArea = ce.getElementNumber();
//                }
//            }
        } else if (numMapAreas > 1) {
            // return the element number of the first selected mapArea
            boolean foundSelectedMapArea = false;
            for (MapArea ma : mapAreas) {
                if (ma.isSelected()) {
                    activeMapArea = ma.getElementNumber();
                    foundSelectedMapArea = true;
                }
            }
            if (!foundSelectedMapArea) {
                // return the top map.
                Collections.sort(mapAreas);
                activeMapArea = mapAreas.get(0).getElementNumber();
            }
            
//            for (CartographicElement ce : listOfCartographicElements) {
//                if (ce instanceof MapArea && ce.isSelected()) {
//                    activeMapArea = ce.getElementNumber();
//                    foundSelectedMapArea = true;
//                }
//            }
//            if (!foundSelectedMapArea) {
//                for (CartographicElement ce : listOfCartographicElements) {
//                    if (ce instanceof MapArea) {
//                        activeMapArea = ce.getElementNumber();
//                    }
//                }
//            }
        } else {
            // you need to add a mapArea
            String name = "MapArea1";
            CartographicElement ce = new MapArea(name);
            addNewCartographicElement(ce);
            findActiveMapArea();
        }
        return activeMapArea;
    }
    
    
    
//    public MapLayer getLayer(int overlayNumber) {
//        try {
//            int i = findLayerIndexByOverlayNum(overlayNumber);
//            return layers.get(i);
//        } catch (Exception e) {
//            return null;
//        }
//    }
    
//    public void addLayer(MapLayer newLayer) {
//        if (activeMapArea < 0) {
//            findMapArea();
//        }
//        if (listOfCartographicElements.size() >= activeMapArea && !(listOfCartographicElements.get(activeMapArea) instanceof MapArea)) {
//            findMapArea();
//        }
//        MapArea mapArea = (MapArea)listOfCartographicElements.get(activeMapArea);
//        mapArea.addLayer(newLayer);
//        //layers.add(newLayer);
//        //numLayers = layers.size();
//        currentExtent = calculateFullExtent();
//        listOfExtents.add(currentExtent.clone());
//        listOfExtentsIndex = listOfExtents.size() - 1;
//        dirty = true;
//    }
    
//    public void removeLayer(int overlayNumber) {
//        try {
//            if (numLayers > 0) {
//                // which layer has an overlayNumber equal to layerNum?
//                int indexOfLayerToRemove = findLayerIndexByOverlayNum(overlayNumber);
//
//                if (indexOfLayerToRemove != -1) { // it exists
//                    if (indexOfLayerToRemove == activeLayerIndex) {
//                        // we're removing the active layer, so we'll need a new one.
//                        // first, are there any other layers?
//                        if (numLayers > 1) {
//                            // if the active layer was the not the bottommost, set the active
//                            // layer to the one currently beneath it, else the one above it.
//                            if (activeLayerIndex > 0) {
//                                activeLayerOverlayNumber--;
//                                activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
//                                activeLayer = layers.get(activeLayerIndex);
//                            } else {
//                                activeLayerOverlayNumber++;
//                                activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
//                                activeLayer = layers.get(activeLayerIndex);
//                            }
//                            layers.remove(indexOfLayerToRemove);
//                            reorderLayers();
//                            // what is the new overlay number and index of the active layer?
//                            activeLayerOverlayNumber = activeLayer.getOverlayNumber();
//                            activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
//                        } else {
//                            // we're removing the active and only layer.
//                            layers.remove(indexOfLayerToRemove);
//                            activeLayer = null;
//                            activeLayerOverlayNumber = -1;
//                            activeLayerIndex = -1;
//                        }
//                    } else {
//                        // we're not removing the active layer. As a result, 
//                        // there must be at least one other layer.
//                        layers.remove(indexOfLayerToRemove);
//                        reorderLayers();
//                        // what is the new overlay number and index of the active layer?
//                        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
//                        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
//                    }
//
//                    numLayers = layers.size();
//                }
//
//                currentExtent = calculateFullExtent();
//                listOfExtents.add(currentExtent.clone());
//            }
//        } catch (Exception e) {
//            // do nothing.
//        }
//    }
    
//    private void reorderLayers() {
//        
//        numLayers = layers.size();
//        
//        if (numLayers == 0) { return; }
//        
//        // find current highest value
//        int highestVal = 0;
//        int highestValIndex = 0;
//        for (int i = 0; i < numLayers; i++) {
//            int overlayNum = layers.get(i).getOverlayNumber();
//            if (overlayNum > highestVal) { 
//                highestVal = overlayNum; 
//                highestValIndex = i;
//            }
//        }
//        
//        int currentLowest = -99;
//        int nextLowest;
//        int nextLowestIndex = 0;
//        
//        int[] orderArray = new int[numLayers];
//        
//        for (int i = 0; i < numLayers; i++) {
//            nextLowest = highestVal;
//            nextLowestIndex = highestValIndex;
//            for (int j = 0; j < numLayers; j++) {
//                int overlayNum = layers.get(j).getOverlayNumber();
//                if ((overlayNum > currentLowest) && (overlayNum < nextLowest)) {
//                    nextLowest = overlayNum;
//                    nextLowestIndex = j;
//                }
//            }
//            currentLowest = nextLowest;
//            orderArray[i] = nextLowestIndex;
//        }
//        
//        for (int i = 0; i < numLayers; i++) {
//            layers.get(orderArray[i]).setOverlayNumber(i);
//        }
//        
//        dirty = true;
//    }
//    
//    public void promoteLayerToTop(int overlayNumber) {
//        if (overlayNumber == numLayers - 1) { // it's already topmost
//            return;
//        }
//        
//        // which layer has an overlayNumber equal to overlayNumber?
//        int layerToMove = findLayerIndexByOverlayNum(overlayNumber);
//        
//        layers.get(layerToMove).setOverlayNumber(numLayers);
//        reorderLayers();
//        
//        // update the active layer overlay number and index.
//        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
//        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
//        
//        dirty = true;
//    }
//   
//    public void demoteLayerToBottom(int overlayNumber) {
//        if (overlayNumber == 0) { // it's already bottommost
//            return;
//        }
//        
//        // which layer has an overlayNumber equal to overlayNumber?
//        int layerToMove = findLayerIndexByOverlayNum(overlayNumber);
//        
//        layers.get(layerToMove).setOverlayNumber(-1);
//        reorderLayers();
//        
//        // update the active layer overlay number and index.
//        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
//        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
//        
//        dirty = true;
//    }
//    
//    public void promoteLayer(int overlayNumber) {
//        if (overlayNumber == numLayers - 1) { // it's already topmost
//            return;
//        }
//        
//        if (numLayers < 2) { // you need at least two layers to promote one
//            return;
//        }
//        
//        int layerToPromote = findLayerIndexByOverlayNum(overlayNumber);
//        int layerToDemote = findLayerIndexByOverlayNum(overlayNumber + 1);
//        layers.get(layerToPromote).setOverlayNumber(overlayNumber + 1);
//        layers.get(layerToDemote).setOverlayNumber(overlayNumber);
//        
//        // update the active layer overlay number and index.
//        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
//        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
//        
//        dirty = true;
//    }
//    
//    public void demoteLayer(int overlayNumber) {
//        if (overlayNumber == 0) {
//            return;
//        }
//        
//        if (numLayers < 2) { // you need at least two layers to demote one
//            return;
//        }
//        
//        int layerToDemote = findLayerIndexByOverlayNum(overlayNumber);
//        int layerToPromote = findLayerIndexByOverlayNum(overlayNumber - 1);
//        layers.get(layerToDemote).setOverlayNumber(overlayNumber - 1);
//        layers.get(layerToPromote).setOverlayNumber(overlayNumber);
//        
//        // update the active layer overlay number and index.
//        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
//        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
//        
//        
//        dirty = true;
//    }
//    
//    public void toggleLayerVisibility(int overlayNumber) {
//        // which layer has an overlayNumber equal to overlayNumber?
//        int layerToToggle = findLayerIndexByOverlayNum(overlayNumber);
//        boolean value = layers.get(layerToToggle).isVisible();
//        if (value) {
//            layers.get(layerToToggle).setVisible(false);
//        } else {
//            layers.get(layerToToggle).setVisible(true);
//        }
//    }
//    
//    public void reversePaletteOfLayer(int overlayNumber) {
//        // which layer has an overlayNumber equal to overlayNumber?
//        int layerToChange = findLayerIndexByOverlayNum(overlayNumber);
//        if (layers.get(layerToChange).getLayerType() == MapLayerType.RASTER) {
//            RasterLayerInfo rli = (RasterLayerInfo)layers.get(layerToChange);
//            boolean value = !rli.isPaletteReversed();
//            rli.setPaletteReversed(value);
//        }
//    }
//    
//    public int findLayerIndexByOverlayNum(int overlayNumber) {
//        // which layer has an overlayNumber equal to layerNum?
//        int layerIndex = -1;
//        for (int i = 0; i < numLayers; i++) {
//            if (layers.get(i).getOverlayNumber() == overlayNumber) {
//                layerIndex = i;
//                break;
//            }
//        }
//        
//        return layerIndex;
//    }
    
//    private String getJson() {
//        //Gson gson = new Gson();
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        String json = gson.toJson(this);
//        return json;
//    }
//    
//    public String[] getCartographicElementsAsJson() {
//        String[] ret = new String[listOfCartographicElements.size()];
//        Gson gson = new Gson(); //new GsonBuilder().setPrettyPrinting().create();
//        for (int i = 0; i < listOfCartographicElements.size(); i++) {  
//            CartographicElement ce = listOfCartographicElements.get(i);
//            if (ce instanceof MapTitle) {
//                ret[i] = gson.toJson(ce);
//            } else if (ce instanceof MapArea) {
//                MapAreaSerializer mas = new MapAreaSerializer((MapArea)ce);
//                ret[i] = mas.getJSON();
//            } else if (ce instanceof NorthArrow) {
//                ret[i] = gson.toJson(ce);
//            }
//        }
//        
//        return ret;
//    }
    
    private boolean save() {
        
        
        
//        System.out.println(getJson());
//        
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        
////        MapInfo alsoMe = gson.fromJson(json, MapInfo.class);
//        String json;
//        for (CartographicElement ce : listOfCartographicElements) {  
//            if (ce instanceof MapTitle) {
//                json = gson.toJson(ce);
//                //System.out.println(json);
//                MapTitle mt = gson.fromJson(json, MapTitle.class);
////                mt.setLabel("Hey dude!");
//            } else if (ce instanceof MapArea) {
//                MapArea ma = (MapArea)(ce);
////                json = ma.getJson();
////                System.out.println(json);
//                json = gson.toJson(ce);
////                System.out.println(json);
//                //MapArea ma = gson.fromJson(json, MapArea.class);
//                
//                //ma.setWidth(1000);
//            } else if (ce instanceof NorthArrow) {
//                json = gson.toJson(ce);
////                System.out.println(json);
//                NorthArrow na = gson.fromJson(json, NorthArrow.class);
//            }
//        }
////        System.out.println(json);
        
        
        
        
//        Gson gson = new Gson();
//        String json = gson.toJson(listOfCartographicElements.get(listOfCartographicElements.size() - 1));
//        
//        System.out.println(json);
//        
//        json = json.replace("\"markerSize\":35", "\"markerSize\":55");
//        NorthArrow na = gson.fromJson(json, NorthArrow.class);
// 
//        String json2 = gson.toJson(na);
//        
        
//        String json3 = gson.toJson((VectorLayerInfo)(mapAreas.get(0).getActiveLayer()));
//        System.out.println(json3);
//        return false;
        
        // I'm disabling this at the moment
        return false;
        
//        if (fileName.equals("")) { return false; }
//        try {
//
//            Element activeMapAreaElement;
//            Element fileNameElement;
//            Element mapNameElement;
//            Element 
//                    
//            Element layertitle;
//            Element isvisible;
//            Element alpha;
//            Element overlayNum;
//            
//            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//
//            // root elements
//            Document doc = docBuilder.newDocument();
//            Element rootElement = doc.createElement("MapInfo");
//            doc.appendChild(rootElement);
//
//            // map elements
//            Element mapElements = doc.createElement("MapElements");
//            rootElement.appendChild(mapElements);
//
//            Element mapname = doc.createElement("MapName");
//            mapname.appendChild(doc.createTextNode(mapName));
//            mapElements.appendChild(mapname);
//
//            if (fullExtent == null) {
//                /************************
//                 * This will need fixing!
//                 */
////                calculateFullExtent();
//            }
//            Element fullextent = doc.createElement("FullExtent");
//            mapElements.appendChild(fullextent);
//            Element feTop = doc.createElement("Top");
//            feTop.appendChild(doc.createTextNode(String.valueOf(fullExtent.getMaxY())));
//            fullextent.appendChild(feTop);
//            Element feBottom = doc.createElement("Bottom");
//            feBottom.appendChild(doc.createTextNode(String.valueOf(fullExtent.getMinY())));
//            fullextent.appendChild(feBottom);
//            Element feLeft = doc.createElement("Left");
//            feLeft.appendChild(doc.createTextNode(String.valueOf(fullExtent.getMinX())));
//            fullextent.appendChild(feLeft);
//            Element feRight = doc.createElement("Right");
//            feRight.appendChild(doc.createTextNode(String.valueOf(fullExtent.getMaxX())));
//            fullextent.appendChild(feRight);
//
//            Element currentextent = doc.createElement("CurrentExtent");
//            mapElements.appendChild(currentextent);
//            Element ceTop = doc.createElement("Top");
//            ceTop.appendChild(doc.createTextNode(String.valueOf(currentExtent.getMaxY())));
//            currentextent.appendChild(ceTop);
//            Element ceBottom = doc.createElement("Bottom");
//            ceBottom.appendChild(doc.createTextNode(String.valueOf(currentExtent.getMinY())));
//            currentextent.appendChild(ceBottom);
//            Element ceLeft = doc.createElement("Left");
//            ceLeft.appendChild(doc.createTextNode(String.valueOf(currentExtent.getMinX())));
//            currentextent.appendChild(ceLeft);
//            Element ceRight = doc.createElement("Right");
//            ceRight.appendChild(doc.createTextNode(String.valueOf(currentExtent.getMaxX())));
//            currentextent.appendChild(ceRight);
//
//            Element activelayer = doc.createElement("ActiveLayerNum");
//            activelayer.appendChild(doc.createTextNode(String.valueOf(getActiveLayerOverlayNumber())));
//            mapElements.appendChild(activelayer);

//            // map layers
//            Element mapLayers = doc.createElement("MapLayers");
//            rootElement.appendChild(mapLayers);
//
//            for (int i = 0; i < layers.size(); i++) {
//                Element maplayer = doc.createElement("Layer");
//                Element layertype = doc.createElement("LayerType");
//                layertype.appendChild(doc.createTextNode(
//                        String.valueOf(layers.get(i).getLayerType())));
//                maplayer.appendChild(layertype);
//                switch (layers.get(i).getLayerType()) {
//                    case RASTER:
//                        RasterLayerInfo rli = (RasterLayerInfo) (layers.get(i));
//                        Element headerfile = doc.createElement("HeaderFile");
//                        headerfile.appendChild(doc.createTextNode(rli.getHeaderFile()));
//                        maplayer.appendChild(headerfile);
//
//                        layertitle = doc.createElement("LayerTitle");
//                        layertitle.appendChild(doc.createTextNode(rli.getLayerTitle()));
//                        maplayer.appendChild(layertitle);
//
//                        isvisible = doc.createElement("IsVisible");
//                        isvisible.appendChild(doc.createTextNode(String.valueOf(rli.isVisible())));
//                        maplayer.appendChild(isvisible);
//
//                        Element paletteReversed = doc.createElement("IsPaletteReversed");
//                        paletteReversed.appendChild(doc.createTextNode(String.valueOf(rli.isPaletteReversed())));
//                        maplayer.appendChild(paletteReversed);
//
//                        Element nonlinearity = doc.createElement("Nonlinearity");
//                        nonlinearity.appendChild(doc.createTextNode(String.valueOf(rli.getNonlinearity())));
//                        maplayer.appendChild(nonlinearity);
//
//                        Element displaymin = doc.createElement("DisplayMinVal");
//                        displaymin.appendChild(doc.createTextNode(String.valueOf(rli.getDisplayMinVal())));
//                        maplayer.appendChild(displaymin);
//
//                        Element displaymax = doc.createElement("DisplayMaxVal");
//                        displaymax.appendChild(doc.createTextNode(String.valueOf(rli.getDisplayMaxVal())));
//                        maplayer.appendChild(displaymax);
//
//                        Element palette = doc.createElement("Palette");
//                        maplayer.appendChild(palette);
//                        palette.appendChild(doc.createTextNode(rli.getPaletteFile()));
//
//                        alpha = doc.createElement("Alpha");
//                        alpha.appendChild(doc.createTextNode(String.valueOf(rli.getAlpha())));
//                        maplayer.appendChild(alpha);
//
//                        overlayNum = doc.createElement("OverlayNum");
//                        overlayNum.appendChild(doc.createTextNode(String.valueOf(rli.getOverlayNumber())));
//                        maplayer.appendChild(overlayNum);
//
//                        break;
//                    case VECTOR:
//                        VectorLayerInfo vli = (VectorLayerInfo) (layers.get(i));
//                        Element shapefile = doc.createElement("ShapeFile");
//                        shapefile.appendChild(doc.createTextNode(vli.getFileName()));
//                        maplayer.appendChild(shapefile);
//
//                        layertitle = doc.createElement("LayerTitle");
//                        layertitle.appendChild(doc.createTextNode(vli.getLayerTitle()));
//                        maplayer.appendChild(layertitle);
//
//                        isvisible = doc.createElement("IsVisible");
//                        isvisible.appendChild(doc.createTextNode(String.valueOf(vli.isVisible())));
//                        maplayer.appendChild(isvisible);
//
//                        alpha = doc.createElement("Alpha");
//                        alpha.appendChild(doc.createTextNode(String.valueOf(vli.getAlpha())));
//                        maplayer.appendChild(alpha);
//
//                        overlayNum = doc.createElement("OverlayNum");
//                        overlayNum.appendChild(doc.createTextNode(String.valueOf(vli.getOverlayNumber())));
//                        maplayer.appendChild(overlayNum);
//
//                        Element fillColour = doc.createElement("FillColour");
//                        fillColour.appendChild(doc.createTextNode(String.valueOf(vli.getFillColour().getRGB())));
//                        maplayer.appendChild(fillColour);
//                        
//                        Element lineColour = doc.createElement("LineColour");
//                        lineColour.appendChild(doc.createTextNode(String.valueOf(vli.getLineColour().getRGB())));
//                        maplayer.appendChild(lineColour);
//                        
//                        Element lineThickness = doc.createElement("LineThickness");
//                        lineThickness.appendChild(doc.createTextNode(String.valueOf(vli.getLineThickness())));
//                        maplayer.appendChild(lineThickness);
//                        
//                        Element markerSize = doc.createElement("MarkerSize");
//                        markerSize.appendChild(doc.createTextNode(String.valueOf(vli.getMarkerSize())));
//                        maplayer.appendChild(markerSize);
//                        
//                        Element markerStyle = doc.createElement("MarkerStyle");
//                        markerStyle.appendChild(doc.createTextNode(String.valueOf(vli.getMarkerStyle().toString())));
//                        maplayer.appendChild(markerStyle);
//                        
//                        Element isFilled = doc.createElement("IsFilled");
//                        isFilled.appendChild(doc.createTextNode(String.valueOf(vli.isFilled())));
//                        maplayer.appendChild(isFilled);
//                        
//                        Element isOutlined = doc.createElement("IsOutlined");
//                        isOutlined.appendChild(doc.createTextNode(String.valueOf(vli.isOutlined())));
//                        maplayer.appendChild(isOutlined);
//                        
//                        Element isDashed = doc.createElement("IsDashed");
//                        isDashed.appendChild(doc.createTextNode(String.valueOf(vli.isDashed())));
//                        maplayer.appendChild(isDashed);
//                        
//                        Element dashArray = doc.createElement("DashArray");
//                        float[] dashArrayFlt = vli.getDashArray();
//                        String dashArrayStr = "";
//                        for (int a = 0; a < dashArrayFlt.length; a++) {
//                            if (a < dashArrayFlt.length - 1) {
//                                dashArrayStr += String.valueOf(dashArrayFlt[a] + ",");
//                            } else {
//                                dashArrayStr += String.valueOf(dashArrayFlt[a]);
//                            }
//                        }
//                        dashArray.appendChild(doc.createTextNode(dashArrayStr));
//                        maplayer.appendChild(dashArray);
//                        
//                        Element isFilledWithOneColour = doc.createElement("IsFilledWithOneColour");
//                        isFilledWithOneColour.appendChild(doc.createTextNode(String.valueOf(vli.isFilledWithOneColour())));
//                        maplayer.appendChild(isFilledWithOneColour);
//                        
//                        Element isOutlinedWithOneColour = doc.createElement("IsOutlinedWithOneColour");
//                        isOutlinedWithOneColour.appendChild(doc.createTextNode(String.valueOf(vli.isOutlinedWithOneColour())));
//                        maplayer.appendChild(isOutlinedWithOneColour);
//                        
//                        Element isPaletteScaled = doc.createElement("IsPaletteScaled");
//                        isPaletteScaled.appendChild(doc.createTextNode(String.valueOf(vli.isPaletteScaled())));
//                        maplayer.appendChild(isPaletteScaled);
//                        
//                        Element paletteFile = doc.createElement("PaletteFile");
//                        paletteFile.appendChild(doc.createTextNode(String.valueOf(vli.getPaletteFile())));
//                        maplayer.appendChild(paletteFile);
//                        
//                        Element colouringAttribute = doc.createElement("ColouringAttribute");
//                        colouringAttribute.appendChild(doc.createTextNode(String.valueOf(vli.getFillAttribute())));
//                        maplayer.appendChild(colouringAttribute);
//                        
//                        break;
//
//                    case MULTISPECTRAL:
//                        /* This will need to be added when support for multispectral
//                         * data has been added.
//                         */
//                        break;
//                }
//                mapLayers.appendChild(maplayer);
//            }
//                
////		// set attribute to staff element
////		Attr attr = doc.createAttribute("id");
////		attr.setValue("1");
////		staff.setAttributeNode(attr);
//// 
////		// shorten way
////		// staff.setAttribute("id", "1");
 
//		// write the content into xml file
//                File file = new File(fileName);
//                if (file.exists()) { file.delete(); }
//                TransformerFactory transformerFactory = TransformerFactory.newInstance();
//                Transformer transformer = transformerFactory.newTransformer();
//                DOMSource source = new DOMSource(doc);
//		StreamResult result = new StreamResult(file);
//                transformer.transform(source, result);
//                return true;
//	  } catch (ParserConfigurationException pce) {
//		System.out.println(pce);
//                return false;
//	  } catch (TransformerException tfe) {
//		System.out.println(tfe);
//                return false;
//	  }   
    }
    
    public boolean saveMap() {
        return save();
    }
    
    public boolean saveMap(String fileName) {
        this.fileName = fileName;
        return save();
    }
    
    private boolean open() {
        
        // this method has to be temporarily disabled.
        return false;
        
//        File file = new File(fileName);
//
//        if (!file.exists()) {
//            return false;
//        }
//        try {                    
//            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//            DocumentBuilder db = dbf.newDocumentBuilder();
//            Document doc = db.parse(file);
//            doc.getDocumentElement().normalize();
//            
//            mapName = doc.getElementsByTagName("MapName").item(0).getTextContent();
//            int activeLayerNum = Integer.parseInt(doc.getElementsByTagName("ActiveLayerNum").item(0).getTextContent());
//            
//            // get the current extent
//            NodeList feList = doc.getElementsByTagName("FullExtent");
//            for (int s = 0; s < feList.getLength(); s++) {
//                if(feList.item(s).getNodeType() == Node.ELEMENT_NODE){
//                    Element el = (Element)feList.item(s);
//                    double top = Double.parseDouble(el.getElementsByTagName("Top").item(0).getTextContent());
//                    double bottom = Double.parseDouble(el.getElementsByTagName("Bottom").item(0).getTextContent());
//                    double right = Double.parseDouble(el.getElementsByTagName("Right").item(0).getTextContent());
//                    double left = Double.parseDouble(el.getElementsByTagName("Left").item(0).getTextContent());
//                    fullExtent = new BoundingBox(left, bottom, right, top);
//                }
//            }
//            
//            // get the current extent
//            NodeList ceList = doc.getElementsByTagName("CurrentExtent");
//            for (int s = 0; s < ceList.getLength(); s++) {
//                if(ceList.item(s).getNodeType() == Node.ELEMENT_NODE){
//                    Element el = (Element)ceList.item(s);
//                    double top = Double.parseDouble(el.getElementsByTagName("Top").item(0).getTextContent());
//                    double bottom = Double.parseDouble(el.getElementsByTagName("Bottom").item(0).getTextContent());
//                    double right = Double.parseDouble(el.getElementsByTagName("Right").item(0).getTextContent());
//                    double left = Double.parseDouble(el.getElementsByTagName("Left").item(0).getTextContent());
//                    currentExtent = new BoundingBox(left, bottom, right, top);
//                }
//            }
//            
//            NodeList layersList = doc.getElementsByTagName("Layer");
//
//            int nlayers = layersList.getLength();
//            
//            for(int s = 0; s < nlayers; s++){
//                if(layersList.item(s).getNodeType() == Node.ELEMENT_NODE){
//                    Element el = (Element)layersList.item(s);
//                    // get the layer type.
//                    String layertype = el.getElementsByTagName("LayerType").item(0).getTextContent();
//                    String layertitle = el.getElementsByTagName("LayerTitle").item(0).getTextContent();
//                    boolean visibility = Boolean.parseBoolean(el.getElementsByTagName("IsVisible").item(0).getTextContent());
//                    int overlayNumber = Integer.parseInt(el.getElementsByTagName("OverlayNum").item(0).getTextContent());
//                    if (layertype.equals("RASTER")) {
//                        String headerFile = el.getElementsByTagName("HeaderFile").item(0).getTextContent();
//                        // see whether it exists, and if it doesn't, see whether a file of the same
//                        // name exists in the working directory or any of its subdirectories.
//                        if (!new File(headerFile).exists()) {
//                            flag = true;
//                            findFile(new File(workingDirectory), new File(headerFile).getName());
//                            if (!retFile.equals("")) {
//                                headerFile = retFile;
//                            } else {
//                                return false;
//                            }
//                        }
//                        
//                        String paletteFile = el.getElementsByTagName("Palette").item(0).getTextContent();
//                        // see whether it exists, and if it doesn't, see whether a file of the same
//                        // name exists in the working directory or an of its subdirectories.
//                        if (!new File(paletteFile).exists()) {
//                            flag = true;
//                            findFile(new File(paletteDirectory), new File(paletteFile).getName());
//                            if (!retFile.equals("")) {
//                                paletteFile = retFile;
//                            } else {
//                                paletteFile = paletteDirectory + "spectrum.pal";
//                            }
//                        }
//                        
//                        int alpha = Integer.parseInt(el.getElementsByTagName("Alpha").item(0).getTextContent());
//                        double nonlinearity = Double.parseDouble(el.getElementsByTagName("Nonlinearity").item(0).getTextContent());
//                        boolean paletteReversed = Boolean.parseBoolean(el.getElementsByTagName("IsPaletteReversed").item(0).getTextContent());
//                        double displaymin = Double.parseDouble(el.getElementsByTagName("DisplayMinVal").item(0).getTextContent());
//                        double displaymax = Double.parseDouble(el.getElementsByTagName("DisplayMaxVal").item(0).getTextContent());
//                        
//                        RasterLayerInfo rli = new RasterLayerInfo(headerFile, paletteFile, alpha, overlayNumber);
//                        rli.setDisplayMinVal(displaymin);
//                        rli.setDisplayMaxVal(displaymax);
//                        rli.setNonlinearity(nonlinearity);
//                        rli.setPaletteReversed(paletteReversed);
//                        rli.setLayerTitle(layertitle);
//                        
//                        layers.add(rli);
//                        numLayers = layers.size();
//        
//                    } else if (layertype.equals("VECTOR")) {
//                        String shapeFile = el.getElementsByTagName("ShapeFile").item(0).getTextContent();
//                        // see whether it exists, and if it doesn't, see whether a file of the same
//                        // name exists in the working directory or an of its subdirectories.
//                        if (!new File(shapeFile).exists()) {
//                            flag = true;
//                            findFile(new File(workingDirectory), new File(shapeFile).getName());
//                            if (!retFile.equals("")) {
//                                shapeFile = retFile;
//                            } else {
//                                return false;
//                            }
//                        }
//                        
//                        int alpha = Integer.parseInt(el.getElementsByTagName("Alpha").item(0).getTextContent());
//                        Color fillColour = new Color(Integer.parseInt(el.getElementsByTagName("FillColour").item(0).getTextContent()));
//                        Color lineColour = new Color(Integer.parseInt(el.getElementsByTagName("LineColour").item(0).getTextContent()));
//                        float lineThickness = Float.parseFloat(el.getElementsByTagName("LineThickness").item(0).getTextContent());
//                        float markerSize = Float.parseFloat(el.getElementsByTagName("MarkerSize").item(0).getTextContent());
//                        boolean isFilled = Boolean.parseBoolean(el.getElementsByTagName("IsFilled").item(0).getTextContent());
//                        boolean isOutlined = Boolean.parseBoolean(el.getElementsByTagName("IsOutlined").item(0).getTextContent());
//                        boolean isDashed = Boolean.parseBoolean(el.getElementsByTagName("IsDashed").item(0).getTextContent());
//                        String[] dashArrayStr = el.getElementsByTagName("DashArray").item(0).getTextContent().split(",");
//                        float[] dashArray = new float[dashArrayStr.length];
//                        for (int a = 0; a < dashArray.length; a++) {
//                            dashArray[a] = Float.parseFloat(dashArrayStr[a]);
//                        }
//                        String markerStyleStr = el.getElementsByTagName("MarkerStyle").item(0).getTextContent();
//                        MarkerStyle markerStyle = PointMarkers.findMarkerStyleFromString(markerStyleStr);
//                        
//                        boolean isFilledWithOneColour = Boolean.parseBoolean(el.getElementsByTagName("IsFilledWithOneColour").item(0).getTextContent());
//                        boolean isOutlinedWithOneColour = Boolean.parseBoolean(el.getElementsByTagName("IsOutlinedWithOneColour").item(0).getTextContent());
//                        boolean isPaletteScaled = Boolean.parseBoolean(el.getElementsByTagName("IsPaletteScaled").item(0).getTextContent());
//                        String colouringAttribute = el.getElementsByTagName("ColouringAttribute").item(0).getTextContent();
//                        String paletteFile = el.getElementsByTagName("PaletteFile").item(0).getTextContent();
//                        // see whether it exists, and if it doesn't, see whether a file of the same
//                        // name exists in the working directory or an of its subdirectories.
//                        if (!new File(paletteFile).exists()) {
//                            flag = true;
//                            findFile(new File(paletteDirectory), new File(paletteFile).getName());
//                            if (!retFile.equals("")) {
//                                paletteFile = retFile;
//                            } else {
//                                paletteFile = paletteDirectory + "spectrum.pal";
//                            }
//                        }
//                        
//                        VectorLayerInfo vli = new VectorLayerInfo(shapeFile, alpha, overlayNumber);
//                        vli.setVisible(visibility);
//                        vli.setFillColour(fillColour);
//                        vli.setLineColour(lineColour);
//                        vli.setLineThickness(lineThickness);
//                        vli.setMarkerSize(markerSize);
//                        vli.setFilled(isFilled);
//                        vli.setOutlined(isOutlined);
//                        vli.setDashed(isDashed);
//                        vli.setDashArray(dashArray);
//                        vli.setMarkerStyle(markerStyle);
//                        vli.setLayerTitle(layertitle);
//                        vli.setFilledWithOneColour(isFilledWithOneColour);
//                        vli.setOutlinedWithOneColour(isOutlinedWithOneColour);
//                        vli.setPaletteScaled(isPaletteScaled);
//                        vli.setPaletteFile(paletteFile);
//                        vli.setFillAttribute(colouringAttribute);
//                        vli.setRecordsColourData();
//                        
//                        layers.add(vli);
//                        numLayers = layers.size();
//        
//                    }
////                    String palette = 
////                    RasterLayerInfo newLayer = new RasterLayerInfo(files[i].toString(), paletteDirectory,
////                        defaultPalettes, 255, openMaps.get(mapNum).getNumLayers());
////                    addLayer(activeLayer);
//                    //Element firstPersonElement = (Element)firstPersonNode;
//                }
//            }
//            
//            setActiveLayer(activeLayerNum);
//            
//            return true;
//        } catch (ParserConfigurationException pce) {
//            System.out.println(pce);
//            return false;
//        } catch (Exception e) {
//            System.out.println(e);
//            return false;
//        }
    }
    
    private transient String retFile = "";
    private transient boolean flag = true;
    private void findFile(File dir, String fileName) {
        if (flag) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    findFile(files[i], fileName);
                } else if (files[i].getName().equals(fileName)) {
                    retFile = files[i].toString();
                    flag = false;
                    break;
                }
            }
        }
    }
    
    public boolean openMap() {
        return open();
    }
    
    public boolean openMap(String fileName) {
        this.fileName = fileName;
        return open();
    }
}