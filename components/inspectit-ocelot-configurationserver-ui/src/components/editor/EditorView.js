import React from 'react'
import { connect } from 'react-redux'
import { configurationSelectors } from '../../redux/ducks/configuration'
import { notificationActions } from '../../redux/ducks/notification'

import dynamic from 'next/dynamic'
import EditorToolbar from './EditorToolbar';

import yamlEditorConfig from '../../data/yaml-editor-config.json'

const AceEditor = dynamic(() => import('./AceEditor'), {
    ssr: false
});

class EditorView extends React.Component {

    initEditor = (editor) => {
        this.editor = editor;
    }

    save = () => {
        this.props.showInfo("File saved", null);
    }

    parsePath = (fullPath) => {
        if (fullPath) {
            const lastIndex = fullPath.lastIndexOf("/") + 1;
            return {
                path: fullPath.slice(0, lastIndex),
                name: fullPath.slice(lastIndex)
            }
        } else {
            return {};
        }
    }

    render() {
        const { selection, isDirectory } = this.props;
        const {path, name} = this.parsePath(selection);
        const icon = "pi-" + (isDirectory ? "folder" : "file");
        const enableButtons = !isDirectory && selection;

        return (
            <div className="this p-grid p-dir-col p-nogutter">
                <style jsx>{`
                .this {
                    flex: 1;
                }
                .this :global(.p-toolbar) {
                    background: 0;
                    border: 0;
                    border-radius: 0;
                    background-color: #eee;
                    border-bottom: 1px solid #ddd;
                }
                .this :global(.p-toolbar-group-right) > :global(*) {
                    margin-left: .25rem;
                }
                .selection-information {
                    display: flex;
                    height: 100%;
                    align-items: center;
                    justify-content: center;
                    color: #bbb;
                }
                .this :global(.p-toolbar-group-left) {
                    font-size: 1rem;
                    display: flex;
                    align-items: center;
                    height: 2rem;
                }
                .this :global(.p-toolbar-group-left) :global(.pi) {
                    font-size: 1.75rem;
                    color: #aaa;
                    margin-right: 1rem;
                }
                .path {
                    color: #999;
                }
                `}</style>
                <div className="p-col-fixed">
                    <EditorToolbar
                        path={path}
                        filename={name}
                        enableButtons={enableButtons}
                        icon={icon}
                        onSave={this.save}
                        onSearch={() => this.editor.execCommand("find")}
                        onHelp={() => this.editor.showKeyboardShortcuts()} />
                </div>
                <div className="p-col">
                    {!selection || isDirectory ?
                        <div className="selection-information">
                            <div>Select a file to start editing.</div>
                        </div>
                        :
                        <AceEditor mode="yaml" theme="cobalt" initEditor={this.initEditor} options={yamlEditorConfig} value={this.props.selection} />
                    }
                </div>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { selection } = state.configuration;
    return {
        selection,
        isDirectory: configurationSelectors.isSelectionDirectory(state)
    }
}

const mapDispatchToProps = {
    showInfo: notificationActions.showInfoMessage
}

export default connect(mapStateToProps, mapDispatchToProps)(EditorView);


