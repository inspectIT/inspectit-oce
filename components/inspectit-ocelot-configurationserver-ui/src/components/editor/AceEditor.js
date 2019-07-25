import React from 'react';
import ace from 'ace-builds/src-noconflict/ace';

import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/ext-language_tools';
import 'ace-builds/src-noconflict/ext-searchbox';
import 'ace-builds/src-noconflict/ext-keybinding_menu';

//include supported themes and modes here
import 'ace-builds/src-noconflict/mode-yaml';
import 'ace-builds/src-noconflict/theme-cobalt';

/**
 * Component which wraps the AceEditor.
 */
class AceEditor extends React.Component {

    constructor(props) {
        super(props);

        this.props.editorRef(this);
        this.divRef = React.createRef();
    }

    render() {
        return (
            <div ref={this.divRef} style={{ width: "100%", height: "100%" }} />
        )
    }

    configureEditor() {
        const { theme, mode, options } = this.props;
        this.editor.setTheme("ace/theme/" + theme)
        this.editor.getSession().setMode("ace/mode/" + mode);

        this.editor.session.off("change", this.onChange);
        this.editor.session.on("change", this.onChange);
        if (options) {
            this.editor.setOptions(options);
        }
    }

    componentDidMount() {
        this.editor = ace.edit(this.divRef.current)
        const editorRef = this.editor;

        ace.config.loadModule("ace/ext/keybinding_menu", function (module) {
            module.init(editorRef);
        })

        this.configureEditor();
        this.updateValue();
    }

    componentDidUpdate() {
        this.configureEditor();
        this.updateValue();
    }

    onChange = (event) => {
        if (this.props.onChange) {
            this.props.onChange(this.getValue());
        }
    }

    /**
     * Updates the editor content using the `value` props and sets the cursor to the beginning of the content.
     */
    updateValue = () => {
        const { value } = this.props;
        const currentValue = this.getValue();
        if (value !== currentValue) {
            this.editor.setValue(this.props.value, -1);
        }
    }

    executeCommand = (command) => {
        this.editor.execCommand(command);
    }

    showShortcuts = () => {
        this.editor.showKeyboardShortcuts();
    }

    getValue = () => {
        return this.editor.getSession().getValue();
    }


}

export default AceEditor;