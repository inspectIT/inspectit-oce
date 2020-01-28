import React from 'react'
import { Tree } from 'primereact/tree';
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors, configurationUtils } from '../../../redux/ducks/configuration'
import { linkPrefix } from '../../../lib/configuration';

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
     * Handle tree selection changes.
     */
    onSelectionChange = (event) => {
        const { selection, rawFiles } = this.props;

        if (event.value !== selection) {
            this.props.selectFile(event.value);
        }
    }

    render() {
        return (
            <div className='this'>
                <style jsx>{`
                    .this {
                        height: 100%;
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
                    value={this.props.files.concat(this.props.defaultTree)}
                    selectionMode="single"
                    selectionKeys={this.props.selection || this.props.defaultSelection}
                    onSelectionChange={this.onSelectionChange}
                />
            </div>

        );
    }
}

function mapStateToProps(state) {
    const { pendingRequests, selection, files, defaultConfig, defaultSelection } = state.configuration;
    return {
        files: configurationSelectors.getFileTree(state),
        loading: pendingRequests > 0,
        selection,
        defaultConfig: defaultConfig,
        defaultTree: configurationSelectors.getDefaultConfigTree(state),
        defaultSelection
    }
}

const mapDispatchToProps = {
    fetchDefaultConfig: configurationActions.fetchDefaultConfig,
    fetchFiles: configurationActions.fetchFiles,
    selectFile: configurationActions.selectFile,
}

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);