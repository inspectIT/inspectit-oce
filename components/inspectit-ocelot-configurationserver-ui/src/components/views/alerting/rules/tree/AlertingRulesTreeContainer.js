import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import * as rulesAPI from '../RulesAPI';
import AlertingRulesToolbar from './AlertingRulesToolbar';
import AlertingRulesTree from './AlertingRulesTree';
import CreateDialog from '../../../../common/dialogs/CreateDialog';
import DeleteDialog from '../../../../common/dialogs/DeleteDialog';

/**
 * The container element of the alerting rules tree, the corresponding toolbar and the action dialogs.
 */
const AlertingRulesTreeContainer = ({ readOnly, onSelectionChanged, selectedRuleName, selectedTemplateName, unsavedRules }) => {
  const [updateDate, setUpdateDate] = useState(Date.now());
  const [isDeleteRuleDialogShown, setDeleteRuleDialogShown] = useState(false);
  const [isCreateRuleDialogShown, setCreateRuleDialogShown] = useState(false);
  const [groupByTemplates, setGroupByTemplates] = useState(true);
  const [groupByTopics, setGroupByTopics] = useState(false);
  const [rules, setRules] = useState(undefined);
  const [templates, setTemplates] = useState(undefined);

  const refreshRulesAndTemplates = () => {
    rulesAPI.fetchAlertingRules(
      (rules) => setRules(rules),
      () => setRules([])
    );
    rulesAPI.fetchAlertingTemplates(
      (templates) => setTemplates(templates),
      () => setTemplates([])
    );
    setUpdateDate(Date.now());
  };

  const ruleDeleted = (ruleName) => {
    refreshRulesAndTemplates();
    if (ruleName === selectedRuleName) {
      onSelectionChanged(undefined, selectedTemplateName);
    }
  };

  useEffect(() => {
    refreshRulesAndTemplates();
  }, [selectedRuleName, selectedTemplateName]);

  return (
    <div className="treeContainer">
      <style jsx>{`
        .treeContainer {
          height: 100%;
          display: flex;
          flex-direction: column;
          border-right: 1px solid #ddd;
        }
        .treeContainer :global(.p-tree) {
          height: 100%;
          border: 0;
          border-radius: 0;
          display: flex;
          flex-direction: column;
          background: 0;
        }
        .treeContainer :global(.details) {
          color: #ccc;
          font-size: 0.75rem;
          text-align: center;
          padding: 0.25rem 0;
        }
      `}</style>
      <AlertingRulesToolbar
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
        groupByTemplates={groupByTemplates}
        groupByTopics={groupByTopics}
        onGroupingChanged={(gbTemplateValue, gbTopicValue) => {
          if (gbTemplateValue !== groupByTemplates) {
            setGroupByTemplates(gbTemplateValue);
          }
          if (gbTopicValue !== groupByTopics) {
            setGroupByTopics(gbTopicValue);
          }
        }}
        onShowDeleteRuleDialog={() => setDeleteRuleDialogShown(true)}
        onShowCreateRuleDialog={() => setCreateRuleDialogShown(true)}
        onRefresh={() => refreshRulesAndTemplates()}
        readOnly={readOnly}
      />
      <AlertingRulesTree
        rules={rules}
        templates={templates}
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
        unsavedRules={unsavedRules}
        onSelectionChanged={onSelectionChanged}
        readOnly={readOnly}
        groupByTemplates={groupByTemplates}
        groupByTopics={groupByTopics}
      />
      <div className="details">Last refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</div>
      <CreateDialog
        categories={templates && templates.map((t) => t.id)}
        useDescription={true}
        title={'Create Alerting Rule'}
        categoryTitle={'Template'}
        elementTitle={'Rule'}
        text={'Create an alerting rule:'}
        categoryIcon={'pi-briefcase'}
        targetElementIcon={'pi-bell'}
        reservedNames={rules && rules.map((r) => r.id)}
        visible={isCreateRuleDialogShown}
        onHide={() => setCreateRuleDialogShown(false)}
        initialCategory={selectedTemplateName}
        onSuccess={(ruleName, templateName, description) => {
          setCreateRuleDialogShown(false);
          rulesAPI.createRule(
            ruleName,
            templateName,
            description,
            (rule) => onSelectionChanged(rule.id, rule.template),
            () => onSelectionChanged(undefined, undefined)
          );
        }}
      />
      <DeleteDialog
        visible={isDeleteRuleDialogShown}
        onHide={() => setDeleteRuleDialogShown(false)}
        name={selectedRuleName}
        text="Delete Rule"
        onSuccess={(ruleName) => rulesAPI.deleteRule(ruleName, (deletedRule) => ruleDeleted(deletedRule))}
      />
    </div>
  );
};

AlertingRulesTreeContainer.propTypes = {
  /**  Name of the selected rule */
  selectedRuleName: PropTypes.string.isRequired,
  /**  Name of the selected template (template in the current context) */
  selectedTemplateName: PropTypes.string.isRequired,
  /**  Whether the contents are read only */
  readOnly: PropTypes.bool,
  /**  List of rules that are unsaved */
  unsavedRules: PropTypes.array.isRequired,
  /**  Callback on changed selection */
  onSelectionChanged: PropTypes.func,
};

AlertingRulesTreeContainer.defaultProps = {
  readOnly: false,
  onSelectionChanged: () => {},
};

export default AlertingRulesTreeContainer;
