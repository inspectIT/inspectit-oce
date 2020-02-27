import yaml from 'js-yaml';
import { Checkbox } from 'primereact/checkbox';
import { Column } from "primereact/column";
import { ColumnGroup } from 'primereact/columngroup';
import { InputText } from 'primereact/inputtext';
import { Menubar } from 'primereact/menubar';
import { Message } from 'primereact/message';
import { Row } from 'primereact/row';
import { TreeTable } from 'primereact/treetable';
import PropTypes from 'prop-types';
import React, { useState } from 'react';


// helper for a schema property type constants
const schemaType = {
    COMPOSITE: 'COMPOSITE',
    STRING: 'STRING',
    INTEGER: 'INTEGER',
    FLOAT: 'FLOAT',
    BOOLEAN: 'BOOLEAN',
    DURATION: 'DURATION',
    ENUM: 'ENUM'
}

/**
 * Editor for showing the config file as the table tree.
 * 
 * TODO what about duration
 * TODO what about enums (select box, but not used)
 * TODO what about the depending props - f.e. ${inspectit.something.something}
 * TODO what about the multiline strings
 */
class TreeTableEditor extends React.Component {

    headerGroup = (
        <ColumnGroup>
            <Row>
                <Column header="Property name" />
                <Column header="Value" />
                <Column header="Nullable" style={{ width: '200px' }} />
                <Column header="Type" style={{ width: '200px' }} />
            </Row>
        </ColumnGroup>
    );

    constructor() {
        super();

        this.state = {
            isError: false,
            data: undefined,
            expandedKeys: { 'inspectit': true }
        }
    }

    componentDidMount() {
        this.regenerateData();
    }

    componentDidUpdate(prevProps) {
        if (prevProps.value !== this.props.value) {
            this.regenerateData();
        }
    }

    regenerateData = () => {
        try {
            const config = yaml.safeLoad(this.props.value);
            const allKeys = {};
            const data = this.processKey(config, [this.props.schema], allKeys);

            this.setState({
                isError: false,
                data,
                expandedKeys: allKeys
            })
        } catch (error) {
            this.setState({
                isError: true,
                data: undefined
            })
        }
    }

    /**
     * Recursive method that returns the array of data to be supplied to the tree table for one config key.
     * 
     * @param config Currently processed configiguration part (from config YAML)
     * @param schemaObjects Array of schema object that could correspond to the given config key
     * @param keysCollector Map to add all keys of created data elements
     * @param parentKeyIdentifier String identifier of the parent key or undifined for root key
     */
    processKey = (config, schemaObjects, keysCollector, parentKeyIdentifier) => {
        const result = [];

        // continue if the schema object has elements only
        if (schemaObjects && schemaObjects.length > 0) {

            // go over all keys of a config object
            Object.keys(config).forEach(congfigKey => {
                // resolve value and the matching schema properties
                const configValue = config[congfigKey];
                const schemaProperty = schemaObjects.find(s => s.propertyName === congfigKey);

                // if we found schema properties create data and push to resulting array
                if (schemaProperty) {
                    const isComposite = schemaProperty.type === schemaType.COMPOSITE;
                    const keyIdentifier = parentKeyIdentifier !== undefined ? parentKeyIdentifier + "." + schemaProperty.propertyName : schemaProperty.propertyName;
                    const children = isComposite && this.processKey(configValue, schemaProperty.children, keysCollector, keyIdentifier) || undefined;

                    const data = {
                        key: keyIdentifier,
                        schemaProperty,
                        value: configValue,
                        selectable: !isComposite,
                        data: {
                            name: schemaProperty.readableName,
                            type: this.getReadableDataType(schemaProperty.type),
                            value: this.getReabableDataValue(configValue, schemaProperty.type),
                            nullable: !isComposite && this.getBoolanRepresentation(schemaProperty.nullable) || ''
                        },
                        children
                    }
                    result.push(data);
                    keysCollector[keyIdentifier] = true;
                } else {
                    const keyIdentifier = parentKeyIdentifier !== undefined ? parentKeyIdentifier + "." + congfigKey : congfigKey;
                    const data = {
                        key: keyIdentifier,
                        selectable: false,
                        data: {
                            name: this.capitalize(congfigKey),
                            type: 'n/a',
                            value: 'Not supported',
                            nullable: 'n/a'
                        },
                        children: []
                    }
                    result.push(data);
                }
            });
        }

        return result;
    }

    capitalize = (string) => string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
    
    getReadableDataType = (type) => type !== schemaType.COMPOSITE ? type.charAt(0) + type.slice(1).toLowerCase() : "";

    getDataType = (type) => type !== "COMPOSITE" ? this.capitalize(type) : "";

    getReabableDataValue = (value, type) => {
        switch (type) {
            case schemaType.BOOLEAN: return this.getBoolanRepresentation(value);
            case schemaType.COMPOSITE: return ""
            default: return value ? value : 'null'
        }
    }

    getBoolanRepresentation = (b) => b ? 'Yes' : 'No';

    expandAll = () => {
        const expandedKeys = {};
        this.expandKeys(this.state.data, expandedKeys);
        this.setState({ expandedKeys });
    }

    expandKeys = (data, map) => {
        if (data) {
            data.forEach(d => {
                map[d.key] = true;
                this.expandKeys(d.children, map);
            });
        }
    }

    menuItems = () => [
        {
            label: 'Expand All',
            icon: 'pi pi-chevron-down',
            disabled: this.state.isError,
            command: this.expandAll
        },
        {
            label: 'Collapse All',
            icon: 'pi pi-chevron-up',
            disabled: this.state.isError,
            command: () => this.setState({ expandedKeys: {} })
        }
    ]

    rowClassName = (data) => {
        return {
            'composite-row': !data.selectable
        }
    }

    /** Editor for string values */
    StringEditor = ({ node, onPropValueChange }) => {
        const defaultValue = node.value;

        // when getting empty value in the text input
        // we eaither set it to null if possible or use the last valid value
        const onChange = (e) => {
            let updateValue = e.target.value;
            if (!updateValue || updateValue.length === 0) {
                updateValue = node.schemaProperty.nullable ? null : `"${defaultValue}"`; 
                onPropValueChange(node.key, updateValue);
            } else {
                onPropValueChange(node.key, `"${updateValue}"`);
            }
        }

        return (
            <InputText type="text" defaultValue={defaultValue} onChange={onChange}  className="value-column" />
        )
    }

    /** Editor for numbers */
    NumberEditor = ({ node, integer, onPropValueChange }) => {
        const defaultValue = node.value;

        // when getting empty value in the text input
        // we eaither set it to null if possible or use the last valid value
        const onChange = (e) => {
            let updateValue = e.target.value;
            if (!updateValue || updateValue.length === 0) {
                updateValue = node.schemaProperty.nullable ? null : defaultValue;
                onPropValueChange(node.key, updateValue);
            } else {
                updateValue = integer ? parseInt(updateValue) : parseFloat(updateValue);
                onPropValueChange(node.key, `${updateValue}`);
            }
        }

        // special key filters for the integers and floats included
        return (
            <InputText type="number" keyfilter={integer && "int" || "num"} defaultValue={defaultValue} onChange={onChange} className="value-column" />
        )
    }

    /** Editor for booleans */
    BooleanEditor = ({ node, onPropValueChange }) => {
        const defaultValue = node.value;
        const [checked, setChecked] = useState(defaultValue);

        // if a boolean can be nullable, then we allow the unchecking of the checkbox
        const onChange = (e) => {
            const { checked, value } = e.target;
            const updateValue = value === true;
            if (checked) {
                setChecked(updateValue);
                onPropValueChange(node.key, `${updateValue}`);
            } else if (node.schemaProperty.nullable) {
                setChecked(undefined);
                onPropValueChange(node.key, null);
            }
        };
        return (
            <div className="p-grid p-nogutter">
                 <div className="p-col-6">
                    <div className="p-grid p-nogutter">
                        <div className="p-col">
                            <Checkbox inputId="rb1" value={true} onChange={onChange} checked={checked === true} />
                        </div>
                        <div className="p-col">
                            <label htmlFor="rb1" className="p-checkbox-label">Yes</label>
                        </div>
                    </div>
                </div>
                <div className="p-col-6">
                    <div className="p-grid p-nogutter">
                        <div className="p-col">
                            <Checkbox inputId="rb2" value={false} onChange={onChange} checked={checked === false} />
                        </div>
                        <div className="p-col">
                            <label htmlFor="rb2" className="p-checkbox-label">No</label>
                        </div>
                    </div>
                </div>
            </div>
        )
    }

    /** No editor - show value only, if not readable then a small warn message is displayed that the data is not editable */
    NoEditor = ({ node, readOnly }) => {
        const value = node.data['value'];
        return (
            <div className="p-grid p-nogutter">
                <div className="p-col">{value && value || ''}</div>
                {
                    value && !readOnly &&
                    <div className="p-col"><Message severity="warn" text="Not editable"></Message></div>
                }
            </div>
        )
    }

    /** returns editor based on the property to be edited */
    getEditor = (props) => {
        // if we are in read only mode return no-editor
        const { readOnly } = this.props;
        const { node } = props;

        if (readOnly) {
            return <this.NoEditor node={node} readOnly/>
        }

        const noEditor = <this.NoEditor node={node} />;

        if (!node.schemaProperty) {
            return noEditor;
        }

        // otherwise return the concrete editor if type is supported
        const { type } = node.schemaProperty;
        switch (type) {
            case schemaType.STRING: return <this.StringEditor node={node} onPropValueChange={this.props.onPropValueChange} />;
            case schemaType.INTEGER: return <this.NumberEditor node={node} integer onPropValueChange={this.props.onPropValueChange} />;
            case schemaType.FLOAT: return <this.NumberEditor node={node} onPropValueChange={this.props.onPropValueChange} />;
            case schemaType.BOOLEAN: return <this.BooleanEditor node={node} onPropValueChange={this.props.onPropValueChange} />;
            default: return noEditor;
        }
    }

    render() {
        const { loading } = this.props;

        return (
            <div className="this">
                <style jsx>{`
                .this {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                }
                .this :global(.p-menubar)  {
                    background-color: #f4f4f4;
                }
                .this :global(.p-menuitem-text)  {
                    font-size: smaller;
                }
                .this :global(.p-menuitem-icon)  {
                    font-size: smaller;
                }
                .errorBox {
                    align-self: center;
                    justify-content: center;
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    color: #bbb;
                }
                .this :global(.composite-row), .this :global(.composite-row) :global(.key-column) {
                    color: grey;
                    font-weight: normal;
                }
                .this :global(.key-column) {
                    color: black;
                    font-weight: bold;
                }
                .this :global(.value-column) {
                    font-family: monospace;
                }
                `}</style>
                {
                    !this.state.isError &&
                    <>
                        <TreeTable
                            value={this.state.data}
                            headerColumnGroup={this.headerGroup}
                            autoLayout
                            scrollable
                            scrollHeight="calc(100vh - 13rem)"
                            loading={loading}
                            expandedKeys={this.state.expandedKeys}
                            onToggle={e => this.setState({ expandedKeys: e.value })}
                            rowClassName={this.rowClassName}
                        >
                            <Column field="name" className="key-column" expander />
                            <Column field="value" className="value-column" editor={this.getEditor} />
                            <Column field="nullable" style={{ width: '200px' }} />
                            <Column field="type" style={{ width: '200px' }} />
                        </TreeTable>
                        <Menubar model={this.menuItems()} />
                    </>
                }
                {
                    this.state.isError &&
                    <div className="errorBox">
                        <p>Properties could not be loaded from the YAML content.</p>
                    </div>
                }
            </div>
        );

    }

}

TreeTableEditor.propTypes = {
    /** The value of the editor (config file) */
    value: PropTypes.string,
    /** The config file schema */
    schema: PropTypes.object,
    /** If there is loading in progress */
    loading: PropTypes.bool,
    /** If it's read only */
    readOnly: PropTypes.bool,
    /** Function to invoke for prop update */
    onPropValueChange: PropTypes.func,
}

TreeTableEditor.defaultProps = {
    loading: false,
};

export default TreeTableEditor;


