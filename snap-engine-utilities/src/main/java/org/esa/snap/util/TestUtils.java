/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.util;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.*;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Utilities for Operator unit tests
 * In order to test the datasets at Array, set the following to true in the nest.config
 * nest.test.ReadersOnAllProducts=true nest.test.ProcessingOnAllProducts=true
 */
public class TestUtils {

    private static final boolean FailOnSkip = false;
    private static final boolean FailOnLargeTestProducts = false;
    private static final boolean FailOnAllNoData = false;
    private static boolean testEnvironmentInitialized = false;
    private static final String SKIPTEST = "skipTest";

    public static final Logger log = BeamLogManager.getSystemLogger();
    private final static String contextID = ResourceUtils.getContextID();
    private static final PropertiesMap testPreferences = Config.getAutomatedTestConfigPropertyMap(contextID + ".tests");

    public final static String rootPathTestProducts;

    public final static String rootPathTerraSarX;
    public final static String rootPathASAR;
    public final static String rootPathRadarsat2;
    public final static String rootPathRadarsat1;
    public final static String rootPathSentinel1;
    public final static String rootPathERS;
    public final static String rootPathJERS;
    public final static String rootPathALOS;
    public final static String rootPathCosmoSkymed;

    private final static int subsetX;
    private final static int subsetY;
    private final static int subsetWidth;
    private final static int subsetHeight;

    private static final int maxIteration;
    private static final String testReadersOnAllProducts;
    private static final String testProcessingOnAllProducts;

    public static final boolean canTestReadersOnAllProducts;
    public static final boolean canTestProcessingOnAllProducts;

    static {
        if(testPreferences != null) {
            rootPathTestProducts = testPreferences.getPropertyPath(contextID + ".test.rootPathTestProducts");
            rootPathTerraSarX = testPreferences.getPropertyPath(contextID + ".test.rootPathTerraSarX");
            rootPathASAR = testPreferences.getPropertyPath(contextID + ".test.rootPathASAR");
            rootPathRadarsat2 = testPreferences.getPropertyPath(contextID + ".test.rootPathRadarsat2");
            rootPathRadarsat1 = testPreferences.getPropertyPath(contextID + ".test.rootPathRadarsat1");
            rootPathSentinel1 = testPreferences.getPropertyPath(contextID + ".test.rootPathSentinel1");
            rootPathERS = testPreferences.getPropertyPath(contextID + ".test.rootPathERS");
            rootPathJERS = testPreferences.getPropertyPath(contextID + ".test.rootPathJERS");
            rootPathALOS = testPreferences.getPropertyPath(contextID + ".test.rootPathALOS");
            rootPathCosmoSkymed = testPreferences.getPropertyPath(contextID + ".test.rootPathCosmoSkymed");

            subsetX = Integer.parseInt(testPreferences.getPropertyString(contextID + ".test.subsetX"));
            subsetY = Integer.parseInt(testPreferences.getPropertyString(contextID + ".test.subsetY"));
            subsetWidth = Integer.parseInt(testPreferences.getPropertyString(contextID + ".test.subsetWidth"));
            subsetHeight = Integer.parseInt(testPreferences.getPropertyString(contextID + ".test.subsetHeight"));

            maxIteration = Integer.parseInt(testPreferences.getPropertyString(contextID + ".test.maxProductsPerRootFolder"));
            testReadersOnAllProducts = testPreferences.getPropertyString(contextID + ".test.ReadersOnAllProducts");
            testProcessingOnAllProducts = testPreferences.getPropertyString(contextID + ".test.ProcessingOnAllProducts");

            canTestReadersOnAllProducts = testReadersOnAllProducts != null && testReadersOnAllProducts.equalsIgnoreCase("true");
            canTestProcessingOnAllProducts = testProcessingOnAllProducts != null && testProcessingOnAllProducts.equalsIgnoreCase("true");
        } else {
            rootPathTestProducts = "";
            rootPathTerraSarX = "";
            rootPathASAR = "";
            rootPathRadarsat2 = "";
            rootPathRadarsat1 = "";
            rootPathSentinel1 = "";
            rootPathERS = "";
            rootPathJERS = "";
            rootPathALOS = "";
            rootPathCosmoSkymed = "";

            subsetX = 0;
            subsetY = 0;
            subsetWidth = 0;
            subsetHeight = 0;

            maxIteration = 0;
            testReadersOnAllProducts = "";
            testProcessingOnAllProducts = "";

            canTestReadersOnAllProducts = false;
            canTestProcessingOnAllProducts = false;
        }
    }

    public static void initTestEnvironment() {
        if (testEnvironmentInitialized)
            return;

        try {
            final RuntimeConfig runtimeConfig = new DefaultRuntimeConfig();

            JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
            MemUtils.configureJaiTileCache();

            //disable JAI media library
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");
            testEnvironmentInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getMaxIterations() {
        return maxIteration;
    }

    public static Product readSourceProduct(final File inputFile) throws IOException {
        if (!inputFile.exists()) {
            throw new IOException(inputFile.getAbsolutePath() + " not found");
        }

        final ProductReader reader = ProductIO.getProductReaderForInput(inputFile);
        if (reader == null)
            throw new IOException("No reader found for " + inputFile);
        return reader.readProductNodes(inputFile, null);
    }

    public static Product createProduct(final String type, final int w, final int h) {
        final Product product = new Product("name", type, w, h);

        product.setStartTime(AbstractMetadata.parseUTC("10-MAY-2008 20:30:46.890683"));
        product.setEndTime(AbstractMetadata.parseUTC("10-MAY-2008 20:35:46.890683"));
        product.setDescription("description");

        addGeoCoding(product);

        AbstractMetadata.addAbstractedMetadataHeader(product.getMetadataRoot());

        return product;
    }

    public static Band createBand(final Product testProduct, final String bandName, final int w, final int h) {
        final Band band = testProduct.addBand(bandName, ProductData.TYPE_INT32);
        band.setUnit(Unit.AMPLITUDE);
        final int[] intValues = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1;
        }
        band.setData(ProductData.createInstance(intValues));
        return band;
    }

    private static void addGeoCoding(final Product product) {

        final TiePointGrid latGrid = new TiePointGrid("lat", 2, 2, 0.5f, 0.5f,
                product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                new float[]{10.0f, 10.0f, 5.0f, 5.0f});
        final TiePointGrid lonGrid = new TiePointGrid("lon", 2, 2, 0.5f, 0.5f,
                product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                new float[]{10.0f, 10.0f, 5.0f, 5.0f},
                TiePointGrid.DISCONT_AT_360);
        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setGeoCoding(tpGeoCoding);
    }

    public static void verifyProduct(final Product product, final boolean verifyTimes, final boolean verifyGeoCoding) throws Exception {
        verifyProduct(product, verifyTimes, verifyGeoCoding, false);
    }

    public static void verifyProduct(final Product product, final boolean verifyTimes, final boolean verifyGeoCoding,
                                     final boolean verifyBandData) throws Exception {
        if (product == null)
            throw new Exception("product is null");
        if (verifyGeoCoding && product.getGeoCoding() == null) {
            log.warning("Geocoding is null for " + product.getFileLocation().getAbsolutePath());
            //throw new Exception("geocoding is null");
        }
        if (product.getMetadataRoot() == null)
            throw new Exception("metadataroot is null");
        if (product.getNumBands() == 0)
            throw new Exception("numbands is zero");
        if (product.getProductType() == null || product.getProductType().isEmpty())
            throw new Exception("productType is null");
        if (verifyTimes) {
            if (product.getStartTime() == null)
                throw new Exception("startTime is null");
            if (product.getEndTime() == null)
                throw new Exception("endTime is null");
        }
        if(verifyBandData && FailOnAllNoData) {
            for (Band b : product.getBands()) {
                if (b.getUnit() == null || b.getUnit().isEmpty())
                    throw new Exception("band " + b.getName() + " has null unit");

                // readPixels gets computeTiles to be executed
                final int w = b.getSceneRasterWidth()/2;
                final int h = b.getSceneRasterHeight()/2;
                if (FailOnLargeTestProducts && (w > subsetWidth*2 || h > subsetHeight*2)) {
                    throw new IOException("Test product too large " + w + "," + h);
                }
                final int x0 = w/2;
                final int y0 = h/2;

                boolean allNoData = true;
                for(int y=y0; y < y0+h; ++y) {
                    final float[] floatValues = new float[w];
                    b.readPixels(x0, y, w, 1, floatValues, ProgressMonitor.NULL);
                    for (float f : floatValues) {
                        if (!(f == b.getNoDataValue() || f == 0 || f == Float.NaN))
                            allNoData = false;
                    }
                }
                if (allNoData) {
                    throw new Exception("band " + b.getName() + " is all no data value");
                }
            }
        }
    }

    public static void attributeEquals(final MetadataElement elem, final String name,
                                       final double trueValue) throws Exception {
        final double val = elem.getAttributeDouble(name, 0);
        if (Double.compare(val, trueValue) != 0) {
            if (Float.compare((float) val, (float) trueValue) != 0)
                throwErr(name + " is " + val + ", expecting " + trueValue);
        }
    }

    public static void attributeEquals(final MetadataElement elem, String name,
                                       final String trueValue) throws Exception {
        final String val = elem.getAttributeString(name, "");
        if (!val.equals(trueValue))
            throwErr(name + " is " + val + ", expecting " + trueValue);
    }

    private static void compareMetadata(final Product testProduct, final Product expectedProduct,
                                        final String[] excemptionList) throws Exception {
        final MetadataElement testAbsRoot = AbstractMetadata.getAbstractedMetadata(testProduct);
        if (testAbsRoot == null)
            throwErr("Metadata is null");
        final MetadataElement expectedAbsRoot = AbstractMetadata.getAbstractedMetadata(expectedProduct);
        if (expectedAbsRoot == null)
            throwErr("Metadata is null");

        if (excemptionList != null) {
            Arrays.sort(excemptionList);
        }

        final MetadataAttribute[] attribList = expectedAbsRoot.getAttributes();
        for (MetadataAttribute expectedAttrib : attribList) {
            if (excemptionList != null && Arrays.binarySearch(excemptionList, expectedAttrib.getName()) >= 0)
                continue;

            final MetadataAttribute result = testAbsRoot.getAttribute(expectedAttrib.getName());
            if (result == null) {
                throwErr("Metadata attribute " + expectedAttrib.getName() + " is missing");
            }
            final ProductData expectedData = result.getData();
            if (!expectedData.equalElems(expectedAttrib.getData())) {
                if ((expectedData.getType() == ProductData.TYPE_FLOAT64 ||
                        expectedData.getType() == ProductData.TYPE_FLOAT64) &&
                        Double.compare(expectedData.getElemDouble(), result.getData().getElemDouble()) == 0) {

                } else if (expectedData.toString().trim().equalsIgnoreCase(result.getData().toString().trim())) {

                } else {
                    throwErr("Metadata attribute " + expectedAttrib.getName() + " expecting " + expectedAttrib.getData().toString()
                            + " got " + result.getData().toString());
                }
            }
        }
    }

    public static void compareProducts(final Product targetProduct,
                                       final Product expectedProduct) throws Exception {
        // compare updated metadata
        compareMetadata(targetProduct, expectedProduct, null);

        if (targetProduct.getNumBands() != expectedProduct.getNumBands())
            throwErr("Different number of bands");

        if (!targetProduct.isCompatibleProduct(expectedProduct, 0))
            throwErr("Geocoding is different");

        for (TiePointGrid expectedTPG : expectedProduct.getTiePointGrids()) {
            final TiePointGrid trgTPG = targetProduct.getTiePointGrid(expectedTPG.getName());
            if (trgTPG == null)
                throwErr("TPG " + expectedTPG.getName() + " not found");

            final float[] expectedTiePoints = expectedTPG.getTiePoints();
            final float[] trgTiePoints = trgTPG.getTiePoints();

            if (!Arrays.equals(trgTiePoints, expectedTiePoints)) {
                throwErr("TPGs are different in file " + expectedProduct.getFileLocation().getAbsolutePath());
            }
        }

        for (Band expectedBand : expectedProduct.getBands()) {

            final Band trgBand = targetProduct.getBand(expectedBand.getName());
            if (trgBand == null)
                throwErr("Band " + expectedBand.getName() + " not found");

            final float[] floatValues = new float[2500];
            trgBand.readPixels(40, 40, 50, 50, floatValues, ProgressMonitor.NULL);

            final float[] expectedValues = new float[2500];
            expectedBand.readPixels(40, 40, 50, 50, expectedValues, ProgressMonitor.NULL);

            if (!Arrays.equals(floatValues, expectedValues)) {
                throwErr("Pixels are different in file " + expectedProduct.getFileLocation().getAbsolutePath());
            }
        }
    }

    public static void comparePixels(final Product targetProduct, final String bandName, final float[] expected) throws IOException {
        comparePixels(targetProduct, bandName, 0, 0, expected);
    }

    public static void comparePixels(final Product targetProduct, final String bandName,
                                     final int x, final int y, final float[] expected) throws IOException {
        final Band band = targetProduct.getBand(bandName);
        assertNotNull(band);

        final float[] floatValues = new float[expected.length];
        band.readPixels(x, y, expected.length, 1, floatValues, ProgressMonitor.NULL);

        for(int i=0; i < expected.length; ++i) {
            assertEquals(expected[i], floatValues[i], 0.0001);
        }
    }

    public static void compareProducts(final Product targetProduct,
                                       final String expectedPath, final String[] excemptionList) throws Exception {

        final Band targetBand = targetProduct.getBandAt(0);
        if (targetBand == null)
            throwErr("targetBand at 0 is null");

        // readPixels: execute computeTiles()
        final float[] floatValues = new float[2500];
        targetBand.readPixels(40, 40, 50, 50, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        final File expectedFile = new File(expectedPath);
        if (!expectedFile.exists()) {
            throwErr("Expected file not found " + expectedFile.toString());
        }

        final ProductReader reader2 = ProductIO.getProductReaderForInput(expectedFile);

        final Product expectedProduct = reader2.readProductNodes(expectedFile, null);
        final Band expectedBand = expectedProduct.getBandAt(0);

        final float[] expectedValues = new float[2500];
        expectedBand.readPixels(40, 40, 50, 50, expectedValues, ProgressMonitor.NULL);
        if (!Arrays.equals(floatValues, expectedValues)) {
            throwErr("Pixels are different in file " + expectedPath);
        }

        // compare updated metadata
        compareMetadata(targetProduct, expectedProduct, excemptionList);
    }

    public static void executeOperator(final Operator op) throws Exception {
        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        // readPixels: execute computeTiles()
        verifyProduct(targetProduct, true, true, true);
    }

    public static Product createSubsetProduct(final Product sourceProduct) throws IOException {
        final int bandWidth = sourceProduct.getSceneRasterWidth();
        final int bandHeight = sourceProduct.getSceneRasterHeight();

        final ProductSubsetBuilder subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();

        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
        subsetDef.addNodeNames(sourceProduct.getBandNames());
        final int w = within(subsetWidth, bandWidth);
        final int h = within(subsetHeight, bandHeight);
        subsetDef.setRegion(within(subsetX, bandWidth-w), within(subsetY, bandHeight-h), w, h);
        subsetDef.setIgnoreMetadata(false);
        subsetDef.setTreatVirtualBandsAsRealBands(false);

        final Product subsetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);
        if(subsetProduct.getSceneRasterWidth() > subsetWidth || subsetProduct.getSceneRasterHeight() > subsetHeight) {
            throw new IOException("product size mismatch");
        }

        return subsetProduct;
    }

    public static Product writeSubsetProduct(final Product sourceProduct) throws IOException {
        final int bandWidth = sourceProduct.getSceneRasterWidth();
        final int bandHeight = sourceProduct.getSceneRasterHeight();

        final ProductSubsetBuilder subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();

        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());

        final String bandName = ProductUtils.findSuitableQuicklookBandName(sourceProduct);
        subsetDef.addNodeNames(new String[]{bandName});
        final int w = within(subsetWidth, bandWidth);
        final int h = within(subsetHeight, bandHeight);
        subsetDef.setRegion(within(subsetX, bandWidth-w), within(subsetY, bandHeight-h), w, h);
        subsetDef.setIgnoreMetadata(false);
        subsetDef.setTreatVirtualBandsAsRealBands(true);

        final Product subsetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);
        final File tmpFile = new File(ResourceUtils.getApplicationUserTempDataDir(), "tmp_subset.dim");
        final WriteOp writer = new WriteOp(subsetProduct, tmpFile, DimapProductConstants.DIMAP_FORMAT_NAME);
        writer.writeProduct(ProgressMonitor.NULL);

        return ProductIO.readProduct(tmpFile);
    }

    private static int within(final int val, final int max) {
        return Math.max(0, Math.min(val, max));
    }

    public static void recurseFindReadableProducts(final File origFolder, final ArrayList<File> productList, int maxCount) throws Exception {


        final File[] folderList = origFolder.listFiles(ProductFunctions.directoryFileFilter);
        for (File folder : folderList) {
            if (!folder.getName().contains(SKIPTEST)) {
                recurseFindReadableProducts(folder, productList, maxCount);
            }
        }

        final File[] fileList = origFolder.listFiles(new ProductFunctions.ValidProductFileFilter());
        for (File file : fileList) {
            if (maxCount > 0 && productList.size() >= maxCount)
                return;

            try {
                final ProductReader reader = ProductIO.getProductReaderForInput(file);
                if (reader != null) {
                    productList.add(file);
                } else {
                    log.warning(file.getAbsolutePath() + " is non valid");
                }
            } catch (Exception e) {
                boolean ok = false;
               /* if(exceptionExemptions != null) {
                    for(String excemption : exceptionExemptions) {
                        if(e.getMessage().contains(excemption)) {
                            ok = true;
                            System.out.println("Excemption for "+e.getMessage());
                            break;
                        }
                    }
                }    */
                if (!ok) {
                    log.severe("Failed to process " + file.toString());
                    throw e;
                }
            }
        }
    }

    public static int recurseProcessFolder(final OperatorSpi spi, final File origFolder, int iterations,
                                           final String[] productTypeExemptions,
                                           final String[] exceptionExemptions) throws Exception {

        final File[] folderList = origFolder.listFiles(ProductFunctions.directoryFileFilter);
        for (File folder : folderList) {
            if (maxIteration > 0 && iterations >= maxIteration)
                break;
            if (!folder.getName().contains(SKIPTEST)) {
                iterations = recurseProcessFolder(spi, folder, iterations, productTypeExemptions, exceptionExemptions);
            }
        }

        final File[] fileList = origFolder.listFiles(new ProductFunctions.ValidProductFileFilter());
        for (File file : fileList) {
            if (maxIteration > 0 && iterations >= maxIteration)
                break;

            try {
                final ProductReader reader = ProductIO.getProductReaderForInput(file);
                if (reader != null) {
                    final Product sourceProduct = reader.readProductNodes(file, null);
                    if (productTypeExemptions != null && containsProductType(productTypeExemptions, sourceProduct.getProductType()))
                        continue;

                    verifyProduct(sourceProduct, true, true, false);

                    final Product subsetProduct = createSubsetProduct(sourceProduct);

                    final Operator op = spi.createOperator();
                    op.setSourceProduct(subsetProduct);

                    TestUtils.log.info(spi.getOperatorAlias() + " Processing " + file.toString());
                    TestUtils.executeOperator(op);

                    ++iterations;
                } else {
                    TestUtils.log.warning(file.getAbsolutePath() + " is non valid");
                }
            } catch (Exception e) {
                boolean ok = false;
                if (exceptionExemptions != null) {
                    for (String exemption : exceptionExemptions) {
                        if (e.getMessage().contains(exemption)) {
                            ok = true;
                            TestUtils.log.info("Exemption for " + e.getMessage());
                            break;
                        }
                    }
                }
                if (!ok) {
                    TestUtils.log.severe("Failed to process " + file.toString());
                    throw e;
                }
            }
        }
        return iterations;
    }

    public static boolean containsProductType(final String[] productTypeExemptions, final String productType) {
        if (productTypeExemptions != null) {
            for (String str : productTypeExemptions) {
                if (productType.contains(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Processes all products in a folder
     *
     * @param spi                   the OperatorSpi to create the operator
     * @param folderPath            the path to recurse through
     * @param productTypeExemptions product types to ignore
     * @param exceptionExemptions   exceptions that are ok and can be ignored for the test
     * @throws Exception general exception
     */
    public static void testProcessAllInPath(final OperatorSpi spi, final String folderPath,
                                            final String[] productTypeExemptions,
                                            final String[] exceptionExemptions) throws Exception {
        if (canTestProcessingOnAllProducts) {
            final File folder = new File(folderPath);
            if (!folder.exists()) {
                skipTest(spi, folderPath+ " not found");
                return;
            }

            int iterations = 0;
            recurseProcessFolder(spi, folder, iterations, productTypeExemptions, exceptionExemptions);
        }
    }

    private final static ProductFunctions.ValidProductFileFilter fileFilter = new ProductFunctions.ValidProductFileFilter(false);

    public static void recurseReadFolder(final Object callingClass, final File origFolder,
                                         final ProductReaderPlugIn readerPlugin,
                                         final ProductReader reader,
                                         final String[] productTypeExemptions,
                                         final String[] exceptionExemptions) throws Exception {
        if (!origFolder.exists()) {
            TestUtils.skipTest(callingClass, "Folder "+origFolder+" not found");
            return;
        }
        recurseReadFolder(origFolder, readerPlugin, reader, productTypeExemptions, exceptionExemptions, 0);
    }

    private static int recurseReadFolder(final File origFolder,
                                        final ProductReaderPlugIn readerPlugin,
                                        final ProductReader reader,
                                        final String[] productTypeExemptions,
                                        final String[] exceptionExemptions,
                                        int iterations) throws Exception {
        final File[] folderList = origFolder.listFiles(ProductFunctions.directoryFileFilter);
        for (File folder : folderList) {
            if (!folder.getName().contains(SKIPTEST)) {
                iterations = recurseReadFolder(folder, readerPlugin, reader, productTypeExemptions, exceptionExemptions, iterations);
                if (maxIteration > 0 && iterations >= maxIteration)
                    return iterations;
            }
        }

        final File[] files = origFolder.listFiles(fileFilter);
        for (File file : files) {
            if (readerPlugin.getDecodeQualification(file) == DecodeQualification.INTENDED) {

                try {
                    log.info("Reading "+ file.toString());

                    final Product product = reader.readProductNodes(file, null);
                    if (productTypeExemptions != null && containsProductType(productTypeExemptions, product.getProductType()))
                        continue;
                    verifyProduct(product, true, true, false);
                    ++iterations;

                    if (maxIteration > 0 && iterations >= maxIteration)
                        break;
                } catch (Exception e) {
                    boolean ok = false;
                    if (exceptionExemptions != null) {
                        for (String excemption : exceptionExemptions) {
                            if (e.getMessage() != null && e.getMessage().contains(excemption)) {
                                ok = true;
                                log.info("Excemption for " + e.getMessage());
                                break;
                            }
                        }
                    }
                    if (!ok) {
                        log.severe("Failed to read " + file.toString());
                        throw e;
                    }
                }
            }
        }
        return iterations;
    }

    public static boolean skipTest(final Object obj) throws Exception {
        return skipTest(obj, "");
    }

    public static boolean skipTest(final Object obj, final String msg) throws Exception {
        log.severe(obj.getClass().getName() + " skipped "+msg);
        if (FailOnSkip) {
            throw new Exception(obj.getClass().getName() + " skipped "+msg);
        }
        return true;
    }

    private static void throwErr(final String description) throws Exception {
        throw new Exception(description);
    }
}
