import { Fieldset } from 'primereact/fieldset';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import React from 'react';
import PropTypes from 'prop-types';

/** data */
import { classMatchers, matcherTypes, tooltipOptions } from './ScopeWizardConstants';

const ClassMatcher = ({ classMatcher, onClassMatcherChange }) => {
  const setState = (stateArgument, value) => {
    const currentClassMatcher = {
      ...classMatcher,
      [stateArgument]: value,
    };

    onClassMatcherChange(currentClassMatcher);
  };

  return (
    <>
      <style jsx>{`
        .row-center {
          display: flex;
          align-items: center;
        }

        .row-center :global(.fill),
        .fill {
          flex-grow: 1;
        }

        .meta-row label {
          margin-right: 2rem;
        }

        .meta-row label:not(:first-child) {
          margin-left: 0.5rem;
        }

        .meta-row .inner-label {
          margin-left: 0.5rem;
          margin-right: 0.5rem;
        }

        .row-margin {
          margin-top: 0.5rem;
        }

        .in-name {
          width: 100%;
        }
      `}</style>

      <Fieldset legend="Class Matcher" style={{ paddingTop: 0, paddingBottom: '1rem' }}>
        <div className="row-center row-margin meta-row">
          <Dropdown
            style={{ width: '12rem' }}
            value={classMatcher.currentClassMatcher}
            options={classMatchers}
            onChange={(e) => setState('currentClassMatcher', e.value)}
            placeholder="Select a Class Matcher"
          />
          <label className="inner-label" htmlFor="which">
            which
          </label>
          <Dropdown
            style={{ width: '14rem' }}
            value={classMatcher.classMatcherType}
            options={matcherTypes}
            onChange={(e) => setState('classMatcherType', e.value)}
            placeholder="Select a Matcher Type"
          />
          <div className="p-inputgroup fill">
            <InputText className="fill" value={classMatcher.className} onChange={(e) => setState('className', e.target.value)} />
            <Button
              tooltip="Class Browser"
              icon="pi pi-search"
              tooltipOptions={tooltipOptions}
              onClick={() => alert('Todo: Open Class Browser')} //showClassBrowserDialog()}
            />
          </div>
        </div>
      </Fieldset>
    </>
  );
};

ClassMatcher.propTypes = {
  /** Class Matcher state */
  classMatcher: PropTypes.object,
  /** Callback on class matcher change */
  onClassMatcherChange: PropTypes.func,
};

ClassMatcher.defaultProps = {
  onClassMatcherChange: () => {},
};

export default ClassMatcher;
