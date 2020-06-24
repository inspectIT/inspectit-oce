import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { promotionActions } from '../../../redux/ducks/promotion';
import { dialogActions } from '../../../redux/ducks/dialog';
import { PROMOTION_CONFLICT_DIALOG } from '../dialogs';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';

/**
 * Dialog for showing promotion conflicts. A conflict can occure if the
 * live branch has been modified and the user tries to promote new files.
 */
const PromotionConflictDialog = () => {
  const dispatch = useDispatch();

  const show = useSelector((state) => state.dialog.show) === PROMOTION_CONFLICT_DIALOG;

  const hideConflictDialog = () => {
    if (show) {
      dispatch(dialogActions.hideDialogs());
    }
  };

  const hideAndRefresh = () => {
    hideConflictDialog();
    dispatch(promotionActions.fetchPromotions());
  };

  const footer = (
    <div>
      <Button label="Continue" onClick={hideConflictDialog} />
      <Button label="Continue and Refresh" className="p-button-secondary" onClick={hideAndRefresh} />
    </div>
  );

  return (
    <Dialog
      header="Concurrent Modification of Configurations"
      visible={show}
      style={{ width: '50vw' }}
      modal={true}
      onHide={() => hideConflictDialog()}
      footer={footer}
    >
      <p>One or more configurations have been promoted in the meantime.</p>
      <p>Your current state is out of sync, thus, cannot be promoted in order to negative side effects.</p>
      <p>Please refresh your promotion files and try again.</p>
    </Dialog>
  );
};

export default PromotionConflictDialog;
