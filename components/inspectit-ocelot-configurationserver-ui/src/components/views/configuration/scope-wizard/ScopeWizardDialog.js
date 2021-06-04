import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import TypeMatcher from './TypeMatcher';
import MethodMatcher from './MethodMatcher';

/** data */
import { DEFAULT_VISIBILITIES } from '../../../editor/method-configuration-editor/constants';

/**
 * The scope wizard dialog itself.
 */
const ScopeWizardDialog = ({ visible, onHide, onApply, scope }) => {
  const [typeMatcher, setTypeMatcher] = useState({ type: 'type', matcherType: 'EQUALS_FULLY', name: null });
  const [methodMatcher, setMethodMatcher] = useState({
    visibilities: _.clone(DEFAULT_VISIBILITIES),
    matcherType: null,
    isConstructor: false,
    isSelectedParameter: false,
    parameterInput: null,
    parameterList: [],
    name: null,
  });

  const [isApplyDisabled, setIsApplyDisabled] = useState(true);
  const [isEditMode, setIsEditMode] = useState(true);

  // Fill out dialog, if scope is not null
  useEffect(() => {
    if (isEditMode && scope) {
      // set type matcher
      const { type, superclass, interfaces } = scope;
      let targetType;
      let targetMatcher;

      if (type) {
        targetType = 'type';
        targetMatcher = type;
      } else if (superclass) {
        targetType = 'superclass';
        targetMatcher = superclass;
      } else if (interfaces && interfaces.length === 1) {
        targetType = 'interfaces';
        targetMatcher = interfaces[0];
      } else {
        // Scopes with multiple type matchers are currently not supported.
        throw new Error('Scopes using multiple type matchers are currently not supported.');
      }

      const preparedTypeMatcher = {
        type: targetType,
        matcherType: targetMatcher['matcher-mode'] ? targetMatcher['matcher-mode'] : 'EQUALS_FULLY',
        name: targetMatcher.name ? targetMatcher.name : null,
      };
      setTypeMatcher(preparedTypeMatcher);

      // set method matcher
      const { methods } = scope;

      if (methods && methods.length === 1) {
        const preparedMethodMatcher = {
          visibilities: methods[0].visibility ? methods[0].visibility : _.clone(DEFAULT_VISIBILITIES),
          matcherType: methods[0]['matcher-mode'] ? methods[0]['matcher-mode'] : methods[0].name ? 'EQUALS_FULLY' : null,
          isConstructor: methods[0]['is-constructor'] ? methods[0]['is-constructor'] : false,
          isSelectedParameter: _.has(methods[0], 'arguments'),
          parameterInput: null,
          parameterList: methods[0].arguments ? methods[0].arguments : [],
          name: methods[0].name ? methods[0].name : null,
        };
        setMethodMatcher(preparedMethodMatcher);
      }

      // exit prefill mode
      setIsEditMode(false);
    }
  });

  // Enable 'apply' button...
  useEffect(() => {
    // ... if class matcher is completely specified
    if (typeMatcher.type && typeMatcher.matcherType && typeMatcher.name) {
      setIsApplyDisabled(false);

      // ... if bot method matcher are completely or not at all specified
      if ((methodMatcher.matcherType && methodMatcher.name) || (!methodMatcher.matcherType && !methodMatcher.name)) {
        setIsApplyDisabled(false);
      }
    }

    // Disable 'apply' button if only one of the method matcher is specified
    if (!methodMatcher.matcherType ^ !methodMatcher.name) {
      setIsApplyDisabled(true);
    }

    // Enable 'apply' button if 'constructor' is specified
    if (methodMatcher.isConstructor) {
      setIsApplyDisabled(false);
    }
  });

  const resetState = () => {
    setTypeMatcher({ type: 'type', matcherType: 'EQUALS_FULLY', name: null });
    setMethodMatcher({
      visibilities: _.clone(DEFAULT_VISIBILITIES),
      matcherType: null,
      isConstructor: false,
      isSelectedParameter: false,
      parameterInput: null,
      parameterList: [],
      name: null,
    });
  };

  const exitDialog = () => {
    resetState();
    setIsEditMode(true);
    onHide();
  };

  const showClassBrowser = () => {
    return alert('Todo: Open Class Browser');
  };

  // the dialogs footer
  const footer = (
    <div>
      <Button
        label="Apply"
        disabled={isApplyDisabled}
        onClick={() => {
          onApply(typeMatcher, methodMatcher);
          exitDialog();
        }}
      />
      <Button label="Cancel" className="p-button-secondary" onClick={exitDialog} />
    </div>
  );

  return (
    <>
      <style jsx>{`
        .this :global(.p-dialog-content) {
          border-left: 1px solid #ddd;
          border-right: 1px solid #ddd;
        }

        .row-center {
          display: flex;
          align-items: center;
        }

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

        .argument-fields-height {
          margin-top: 0.5rem;
        }

        .this :global(.method-matcher-dropdown) {
          width: 14rem;
        }

        .this :global(.in-name) {
          width: 100%;
        }
      `}</style>

      <div className="this">
        <Dialog
          className="scope-wizard-dialog"
          header="Define Selection"
          visible={visible}
          style={{ width: '50rem', minWidth: '50rem' }}
          onHide={exitDialog}
          blockScroll
          footer={footer}
          focusOnShow={false}
        >
          <TypeMatcher typeMatcher={typeMatcher} onTypeMatcherChange={setTypeMatcher} onShowClassBrowser={showClassBrowser} />
          <MethodMatcher methodMatcher={methodMatcher} onMethodMatcherChange={setMethodMatcher} />
        </Dialog>
      </div>
    </>
  );
};

ScopeWizardDialog.propTypes = {
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
  /** Callback on dialog apply */
  onApply: PropTypes.func,
  /** JSON model of the scope when in edit mode */
  scope: PropTypes.object,
};

ScopeWizardDialog.defaultProps = {
  visible: true,
  onHide: () => {},
  onApply: () => {},
  scope: null,
};

export default ScopeWizardDialog;
