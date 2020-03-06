import React from 'react';
import { Tree } from 'primereact/tree';
import { connect } from 'react-redux';
import { configurationActions, configurationSelectors, configurationUtils } from '../../../redux/ducks/configuration';
import { linkPrefix } from '../../../lib/configuration';

import { DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';
import { filter } from 'lodash';

/**
 * The file tree used in the configuration view.
 */
class FileTree extends React.Component {

    /**
     * Fetch the files initially.
     */
    componentDidMount = () => {

        const { loading, defaultConfig } = this.props;
        if (!loading) {
            this.props.fetchFiles();
        }

        if (Object.entries(defaultConfig).length === 0) {
            this.props.fetchDefaultConfig();
        }
    }

    /**
     * Arranges first directories and then files. Within the directories or files it is an alphabetical sorting.
     */
    sortFiles = (allFiles) => {

        allFiles.sort(function (first, second) {
            const labelFirst = first.label.toUpperCase();
            const labelSecond = second.label.toUpperCase();
            if (labelFirst < labelSecond) {
                return -1;
            }
            if (labelFirst > labelSecond) {
                return 1;
            }
            return 0;
        });

        let directories = [];
        let files = [];

        allFiles.forEach(element => {
            if (element.children !== undefined) {
                directories.push(element)
                if (element.children.length > 0) {
                    element.children = this.sortFiles(element.children);
                }
            }
            else {
                files.push(element);
            }
        })
        return directories.concat(files);
    }

    /**
     * Handle tree selection changes.
     */
    onSelectionChange = (event) => {
        const { selection, selectedDefaultConfigFile, rawFiles } = this.props;
        const newSelection = event.value;
        if (newSelection) {
            if (newSelection !== selection && newSelection !== selectedDefaultConfigFile) {
                this.props.selectFile(newSelection);
            }
        } else {
            if (selection || selectedDefaultConfigFile) {
                this.props.selectFile(null);
            }
        }
    }

    /**
     * Handle drag and drop movement.
     */
    onDragDrop = (event) => {
        const newTree = event.value.filter(node => node.key !== DEFAULT_CONFIG_TREE_KEY)
        const paths = this.comparePaths('', newTree);

        if (paths) {
            const { source, target } = paths;
            this.props.move(source, target, true);
        }
    }

    /**
     * Attempt to find a file in the 'wrong' place by comparing a node's key with it's expected key.
     * Returns the old (source) and expected (target) key when a node is found.
     */
    comparePaths = (parentKey, nodes) => {
        let foundFile = filter(nodes, file => file.key !== `${parentKey}/${file.label}`);
        if (foundFile.length === 1) {
            return {
                source: foundFile[0].key,
                target: `${parentKey}/${foundFile[0].label}`
            };
        }

        for (const child of nodes) {
            if (child.children) {
                const res = this.comparePaths(child.key, child.children);
                if (res) {
                    return res;
                }
            }
        }

        return null;
    }

    render() {
        return (
            <div className='this'>
                <style jsx>{`
                    .this {
                        overflow: auto;
                        flex-grow: 1;
                    }
                    .this :global(.cm-tree-icon) {
                        width: 1.3rem;
                        height: 1.3rem;
                    }
                    .this :global(.cm-tree-label) {
                        color: #aaa;
                    }
                    .this :global(.ocelot-tree-head-orange) {
                        background: url("${linkPrefix}/static/images/inspectit-ocelot-head_orange.svg") center no-repeat;
                        background-size: 1rem 1rem;
                    }
                    .this :global(.ocelot-tree-head-white) {
                        background: url("${linkPrefix}/static/images/inspectit-ocelot-head_white.svg") center no-repeat;
                        background-size: 1rem 1rem;
                    }
				`}</style>
                <Tree
                    className={this.props.className}
                    filter={true}
                    filterBy="label"
                    value={this.props.defaultTree.concat(this.sortFiles(this.props.files))}
                    selectionMode="single"
                    selectionKeys={this.props.selection || this.props.selectedDefaultConfigFile}
                    onSelectionChange={this.onSelectionChange}
                    dragdropScope="config-file-tree"
                    onDragDrop={this.onDragDrop}
                />
            </div>

        );
    }
}

function mapStateToProps(state) {
    const { pendingRequests, selection, files, defaultConfig, selectedDefaultConfigFile } = state.configuration;
    return {
        files: configurationSelectors.getFileTree(state),
        loading: pendingRequests > 0,
        selection,
        defaultConfig: defaultConfig,
        defaultTree: configurationSelectors.getDefaultConfigTree(state),
        selectedDefaultConfigFile
    }
}

const mapDispatchToProps = {
    fetchDefaultConfig: configurationActions.fetchDefaultConfig,
    fetchFiles: configurationActions.fetchFiles,
    selectFile: configurationActions.selectFile,
    move: configurationActions.move
}

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);