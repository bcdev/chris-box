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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

class FileEditor extends PropertyEditor {

    private FileEditor() {
    }

    // we do not want to add this editor to the {@code com.bc.ceres.swing.binding.PropertyEditorRegistry}
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
        final ComponentAdapter adapter = new TextComponentAdapter(textField);
        final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
        final JPanel editorPanel = new JPanel(new BorderLayout(2, 2));

        final Object directory = propertyDescriptor.getAttribute("directory");
        final Object choosableFileFilter = propertyDescriptor.getAttribute("choosableFileFilter");

        final JButton etcButton = new JButton("...");
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
