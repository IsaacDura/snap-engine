/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * The panel in the binning operator UI which allows for specifying the configuration of binning variables.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class BinningVariablesPanel extends JPanel {

    private final AppContext appContext;
    private final BinningFormModel binningFormModel;
    private VariableConfigTable bandsTable;
    private double currentResolution;

    BinningVariablesPanel(AppContext appContext, BinningFormModel binningFormModel) {
        this.appContext = appContext;
        this.binningFormModel = binningFormModel;
        TableLayout layout = new TableLayout(1);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        setLayout(layout);
        add(createBandsPanel());
        add(createParametersPanel());
    }

    private JPanel createBandsPanel() {
        bandsTable = new VariableConfigTable(binningFormModel, appContext);
        final JPanel bandsPanel = new JPanel(new GridBagLayout());

        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"), false);
        final AbstractButton removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"), false);
        final AbstractButton copyButton = ToolButtonFactory.createButton(new CopyRowAction(bandsTable), false);
        copyButton.setEnabled(false);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bandsTable.addNewRow();
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bandsTable.removeSelectedRows();
            }
        });
        bandsTable.addSelectionListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                copyButton.setEnabled(bandsTable.canDuplicate());
            }
        });

        addButton.setToolTipText("Add new aggregation");
        removeButton.setToolTipText("Remove selected aggregation");
        copyButton.setToolTipText("Clone selected aggregation");

        final GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(bandsPanel, addButton, gbc, "gridx=0");
        GridBagUtils.addToPanel(bandsPanel, removeButton, gbc, "gridx=1");
        GridBagUtils.addToPanel(bandsPanel, copyButton, gbc, "gridx=2");
        GridBagUtils.addHorizontalFiller(bandsPanel, gbc);
        GridBagUtils.addToPanel(bandsPanel, bandsTable.getComponent(), gbc, "gridx=0,gridy=1,gridwidth=4,fill=BOTH,weightx=1,weighty=1");

        return bandsPanel;
    }

    private JPanel createParametersPanel() {
        final JButton validPixelExpressionButton = new JButton("...");
        final Dimension preferredSize = validPixelExpressionButton.getPreferredSize();
        preferredSize.setSize(25, preferredSize.getHeight());
        validPixelExpressionButton.setPreferredSize(preferredSize);
        validPixelExpressionButton.setEnabled(hasSourceProducts());
        binningFormModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS)
                    || evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCT_PATHS)) {
                    validPixelExpressionButton.setEnabled(hasSourceProducts());
                }
            }
        });
        final JTextField validPixelExpressionField = new JTextField();
        validPixelExpressionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final String expression = editExpression(validPixelExpressionField.getText());
                if (expression != null) {
                    validPixelExpressionField.setText(expression);
                    try {
                        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_EXPRESSION, expression);
                    } catch (ValidationException e) {
                        appContext.handleError("Invalid expression", e);
                    }
                }
            }
        });

        final JTextField numPixelsTextField = new IntegerTextField(BinningFormModel.DEFAULT_NUM_ROWS);

        String defaultResolution = getString(computeResolution(BinningFormModel.DEFAULT_NUM_ROWS));
        final JTextField resolutionTextField = new DoubleTextField(defaultResolution);
        JButton resolutionButton = new JButton("original");

        final JTextField superSamplingTextField = new IntegerTextField(1);

        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT, Integer.class));
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, Integer.class));

        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT, numPixelsTextField);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, superSamplingTextField);

        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT).setPropertyValue(BinningFormModel.DEFAULT_NUM_ROWS);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING).setPropertyValue(1);

        binningFormModel.getBindingContext().getPropertySet().getProperty(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT).addPropertyChangeListener(
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateResolutionLabel(numPixelsTextField, resolutionTextField);
                    }
                }
        );

        ResolutionTextFieldListener listener = new ResolutionTextFieldListener(resolutionTextField, numPixelsTextField);
        resolutionTextField.addFocusListener(listener);
        resolutionTextField.addActionListener(listener);
        resolutionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Product[] sourceProducts = binningFormModel.getSourceProducts();
                if (sourceProducts.length > 0) {
                    /*

                    todo: compute resolution of first product, set to resolutionTextField

                     */
                }
            }
        });

        final Component validPixelExpressionToolTip = new JLabel(UIUtils.loadImageIcon("icons/Help16.gif"));
        final Component numPixelsToolTip = new JLabel(UIUtils.loadImageIcon("icons/Help16.gif"));
        final Component resolutionToolTip = new JLabel(UIUtils.loadImageIcon("icons/Help16.gif"));
        final Component supersamplingToolTip = new JLabel(UIUtils.loadImageIcon("icons/Help16.gif"));

        TableLayout layout = new TableLayout(4);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableWeightX(0.0);
        layout.setCellColspan(1, 1, 2);
        layout.setCellColspan(3, 1, 2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(1, 1.0);
        layout.setTablePadding(10, 5);

        final JPanel parametersPanel = new JPanel(layout);

        parametersPanel.add(new JLabel("Valid pixel expression:"));
        parametersPanel.add(validPixelExpressionField);
        parametersPanel.add(validPixelExpressionButton);
        parametersPanel.add(validPixelExpressionToolTip);

        parametersPanel.add(new JLabel("#Pixels (90N - 90S):"));
        parametersPanel.add(numPixelsTextField);
        parametersPanel.add(numPixelsToolTip);

        parametersPanel.add(new JLabel("Spatial resolution (km/px):"));
        parametersPanel.add(resolutionTextField);
        parametersPanel.add(resolutionButton);
        parametersPanel.add(resolutionToolTip);

        parametersPanel.add(new JLabel("Supersampling:"));
        parametersPanel.add(superSamplingTextField);
        parametersPanel.add(supersamplingToolTip);

        return parametersPanel;
    }

    private static int computeNumRows(double resolution) {
        final double RE = 6378.145;
        int numRows = (int) ((RE * Math.PI) / resolution) + 1 ;
        return numRows % 2 == 0 ? numRows : numRows + 1;
    }

    private static double computeResolution(int numRows) {
        final double RE = 6378.145;
        return (RE * Math.PI) / (numRows - 1);
    }

    private static void updateResolutionLabel(JTextField targetHeightTextField, JTextField resolutionField) {
        resolutionField.setText(getResolutionString(Integer.parseInt(targetHeightTextField.getText())));
    }

    static String getResolutionString(int numRows) {
        double number = computeResolution(numRows);
        return getString(number);
    }

    private static String getString(double number) {
        final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator('.');
        final DecimalFormat decimalFormat = new DecimalFormat("#.##", formatSymbols);
        return decimalFormat.format(number);
    }

    private boolean hasSourceProducts() {
        return binningFormModel.getSourceProducts().length > 0;
    }

    private String editExpression(String expression) {
        final Product product = binningFormModel.getSourceProducts()[0];
        if (product == null) {
            return null;
        }
        final ProductExpressionPane expressionPane;
        expressionPane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product,
                                                                           appContext.getPreferences());
        expressionPane.setCode(expression);
        final int i = expressionPane.showModalDialog(appContext.getApplicationWindow(), "Expression Editor");
        if (i == ModalDialog.ID_OK) {
            return expressionPane.getCode();
        }
        return null;
    }

    private static class IntegerTextField extends JTextField {

        private final static String disallowedChars = "`§~!@#$%^&*()_+=\\|\"':;?/>.<,- ";

        public IntegerTextField(int defaultValue) {
            super(defaultValue + "");
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            if (!Character.isLetter(e.getKeyChar()) && disallowedChars.indexOf(e.getKeyChar()) == -1) {
                super.processKeyEvent(e);
            }
        }
    }

    private static class CopyRowAction extends AbstractAction {

        private final ImageIcon imageIcon;
        private VariableConfigTable bandsTable;

        private CopyRowAction(VariableConfigTable bandsTable) {
            this.bandsTable = bandsTable;
            this.imageIcon = UIUtils.loadImageIcon("icons/Copy24.gif");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            bandsTable.duplicateSelectedRow();
        }

        @Override
        public Object getValue(String key) {
            if (key.equals(LARGE_ICON_KEY)) {
                return imageIcon;
            }
            return super.getValue(key);
        }
    }

    private class DoubleTextField extends JTextField {

        private final static String disallowedChars = "`§~!@#$%^&*()_+=\\|\"':;?/><,- ";

        public DoubleTextField(String defaultValue) {
            super(defaultValue);
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            if (!Character.isLetter(e.getKeyChar()) && disallowedChars.indexOf(e.getKeyChar()) == -1) {
                super.processKeyEvent(e);
            }
        }
    }

    private class ResolutionTextFieldListener extends FocusAdapter implements ActionListener {

        private final JTextField resolutionTextField;
        private final JTextField numPixelsTextField;

        public ResolutionTextFieldListener(JTextField resolutionTextField, JTextField numPixelsTextField) {
            this.resolutionTextField = resolutionTextField;
            this.numPixelsTextField = numPixelsTextField;
        }

        @Override
        public void focusLost(FocusEvent e) {
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            update();
        }

        private void update() {
            double resolution = Double.parseDouble(resolutionTextField.getText());
            if (Math.abs(currentResolution - resolution) > 1E-6) {
                numPixelsTextField.setText(String.valueOf(computeNumRows(resolution)));
                currentResolution = resolution;
            }
        }
    }
}
