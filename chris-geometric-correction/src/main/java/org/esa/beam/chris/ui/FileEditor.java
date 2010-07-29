/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.chris.ui;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import org.esa.beam.util.io.FileChooserFactory;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * File editor.
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
class FileEditor extends PropertyEditor {

    private FileEditor() {
    }

    // we do not want to add this editor to the {@code com.bc.ceres.swing.binding.PropertyEditorRegistry}
    // todo: - rq/rq merge this file editor with {@code com.bc.ceres.swing.binding.internal.FileEditor}

    static FileEditor getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        return File.class.isAssignableFrom(propertyDescriptor.getType());
    }

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        final JTextField textField = new JTextField();
        textField.setColumns(30);
        final ComponentAdapter adapter = new TextComponentAdapter(textField);
        final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
        final JPanel editorPanel = new JPanel(new BorderLayout(2, 2));

        final Object directory = propertyDescriptor.getAttribute("directory");
        final Object choosableFileFilter = propertyDescriptor.getAttribute("choosableFileFilter");

        final JButton etcButton = new JButton("...");
        final Dimension size = new Dimension(26, 16);
        etcButton.setPreferredSize(size);
        etcButton.setMinimumSize(size);
        etcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final FileChooserFactory chooserFactory = FileChooserFactory.getInstance();
                final JFileChooser chooser;
                if (Boolean.TRUE.equals(directory)) {
                    chooser = chooserFactory.createDirChooser((File) binding.getPropertyValue());
                } else {
                    chooser = chooserFactory.createFileChooser((File) binding.getPropertyValue());
                }
                if (choosableFileFilter instanceof FileFilter) {
                    chooser.addChoosableFileFilter((FileFilter) choosableFileFilter);
                }
                final int answer = chooser.showDialog(editorPanel, "Select");
                if (answer == JFileChooser.APPROVE_OPTION) {
                    binding.setPropertyValue(chooser.getSelectedFile());
                }
            }
        });
        editorPanel.add(textField, BorderLayout.CENTER);
        editorPanel.add(etcButton, BorderLayout.EAST);

        return editorPanel;
    }

    // holder idiom

    private static class Holder {

        private static final FileEditor INSTANCE = new FileEditor();
    }
}
