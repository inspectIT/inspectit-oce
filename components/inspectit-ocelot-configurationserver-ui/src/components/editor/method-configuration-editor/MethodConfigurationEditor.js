import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputSwitch } from 'primereact/inputswitch';
import { Button } from 'primereact/button';
import yaml from 'js-yaml';
import _ from 'lodash';
import SelectionInformation from '../SelectionInformation';

/**
 * GUI editor for creating method/configurations.
 */
const MethodConfigurationEditor = ({ yamlConfiguration }) => {
  // state variables
  const [scopes, setScopes] = useState([]);
  const [expandedRows, setExpandedRows] = useState([]);
  const [scopeStates, setScopeStates] = useState({});

  // derrived variables
  const scopesExist = scopes.length > 0;

  useEffect(() => {
    // parse configuration
    const configuration = yaml.safeLoad(yamlConfiguration);

    // collect existing scopes from the configuration
    const scopeObjects = _.get(configuration, 'inspectit.instrumentation.scopes');
    const currentScopes = _.map(scopeObjects, (value, key) => {
      const { type, superclass, interfaces } = value;
      return {
        typeKey: JSON.stringify(type) + '|' + JSON.stringify(superclass) + '|' + JSON.stringify(interfaces),
        name: key,
        scope: value,
      };
    });
    setScopes(currentScopes);

    // expand all rows by default
    const initialExpandedRows = _.reduce(
      currentScopes,
      (result, { typeKey }) => {
        result[typeKey] = true;
        return result;
      },
      {}
    );
    setExpandedRows(initialExpandedRows);
  }, []);

  /**
   * Sets the scope state attribute with the specified name of the scope with the given
   * name to the provided value.
   *
   * @param {*} scopeName the scope's name
   * @param {*} attribute the attribute to set
   * @param {*} value the desired value
   */
  const setScopeStateAttribute = (scopeName, attribute, value) => {
    const scopeState = _.get(scopeStates, scopeName, {});
    setScopeStates({
      ...scopeStates,
      [scopeName]: {
        ...scopeState,
        [attribute]: value,
      },
    });
  };

  /**
   * Providing the template body for the row group header.
   */
  const rowGroupHeaderTemplate = ({ typeKey }) => {
    return 'Target: ' + typeKey;
  };

  /**
   * Providing the template body for the scopes.
   */
  const scopeDescriptionBodyTemplate = ({ scope }) => {
    return JSON.stringify(scope);
  };

  /**
   * Providing a generic body template for a scopes state variables (tracing, measuring, ...).
   * @param {*} scopeName the name of the target scope
   * @param {*} stateAttribute  the attribute name to set
   */
  const scopeStateBodyTemplate = (scopeName, stateAttribute) => {
    const scopeState = _.get(scopeStates, scopeName, {});

    return (
      <InputSwitch
        checked={scopeState[stateAttribute]}
        onChange={(e) => {
          setScopeStateAttribute(scopeName, stateAttribute, e.value);
        }}
      />
    );
  };

  /**
   * Providing the template body for the scope's control buttons.
   */
  const scopeEditBodyTemplate = ({ name }) => {
    return (
      <div align="right">
        <Button icon="pi pi-pencil" style={{ marginRight: '0.5rem' }} />
        <Button icon="pi pi-trash" />
      </div>
    );
  };

  return (
    <>
      <style jsx>{`
        .this {
          display: flex;
          flex-direction: column;
          height: 100%;
        }

        .this :global(.p-datatable th) {
          border: none;
          text-align: left;
        }

        .this :global(.p-datatable tr td) {
          border: none;
        }

        .this :global(.p-datatable tr) {
          border-top: 1px solid #e7e7e7;
        }

        .this :global(.p-datatable .p-rowgroup-header td) {
          padding: 1rem 0.5rem;
        }

        .this :global(.p-datatable-row td:first-of-type) {
          padding-left: 2.25rem;
        }

        /* prevent different backgrounds for even and odd row numbers*/
        .this :global(.p-datatable .p-datatable-tbody > tr) {
          background-color: white;
        }

        .no-content-hint {
          background-color: red;
          flex-grow: 1;
        }
      `}</style>
      <div className="this">
        {scopesExist ? (
          <DataTable
            value={scopes}
            rowHover
            dataKey="typeKey"
            sortField="typeKey"
            sortOrder={1}
            groupField="typeKey"
            expandableRowGroups={true}
            expandedRows={expandedRows}
            onRowToggle={(e) => setExpandedRows(e.data)}
            rowGroupHeaderTemplate={rowGroupHeaderTemplate}
            rowGroupFooterTemplate={() => <></>}
            rowGroupMode="subheader"
          >
            <Column body={scopeDescriptionBodyTemplate} header="Target" />
            <Column body={({ name }) => scopeStateBodyTemplate(name, 'tracing')} header="Trace" style={{ width: '6rem' }}></Column>
            <Column body={({ name }) => scopeStateBodyTemplate(name, 'measure')} header="Measure" style={{ width: '6rem' }}></Column>
            <Column body={scopeEditBodyTemplate} style={{ width: '8rem' }}></Column>
          </DataTable>
        ) : (
          <SelectionInformation hint="The configuration is empty." />
        )}
      </div>
    </>
  );
};

MethodConfigurationEditor.propTypes = {
  /** The configuration to show in a YAML representation.*/
  yamlConfiguration: PropTypes.string,
};

MethodConfigurationEditor.defaultProps = {
  yamlConfiguration: null,
};

export default MethodConfigurationEditor;
