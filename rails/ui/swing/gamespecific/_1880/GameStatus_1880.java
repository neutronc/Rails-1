/**
 * 
 */
package rails.ui.swing.gamespecific._1880;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import rails.common.LocalText;
import rails.game.Bank;
import rails.game.PublicCompanyI;
import rails.game.action.BuyCertificate;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import rails.game.action.StartCompany;
import rails.game.correct.CashCorrectionAction;
import rails.sound.SoundManager;
import rails.ui.swing.GameStatus;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.elements.ClickField;
import rails.ui.swing.elements.RadioButtonDialog;
import rails.game.specific._1880.*;

/**
 * @author Martin Brumm
 *
 */
public class GameStatus_1880 extends GameStatus {
       private static final long serialVersionUID = 1L;
    
    /**
     * 
     */
    public GameStatus_1880() {
       super();  
    }
    
@Override
    public void actionPerformed(ActionEvent actor) {
        JComponent source = (JComponent) actor.getSource();
        List<PossibleAction> actions;
        PossibleAction chosenAction = null;

        if (source instanceof ClickField) {
            gbc = gb.getConstraints(source);
            actions = ((ClickField) source).getPossibleActions();

            //notify sound manager that click field has been selected
            SoundManager.notifyOfClickFieldSelection(actions.get(0));

            // Assume that we will have either sell or buy actions
            // under one ClickField, not both. This seems guaranteed.
            log.debug("Action is " + actions.get(0).toString());

            if (actions == null || actions.size() == 0) {

                log.warn("No ClickField action found");

            } else if (actions.get(0) instanceof SellShares) {

                List<String> options = new ArrayList<String>();
                List<SellShares> sellActions = new ArrayList<SellShares>();
                List<Integer> sellAmounts = new ArrayList<Integer>();
                SellShares sale;
                for (PossibleAction action : actions) {
                    sale = (SellShares) action;

                    //for (int i = 1; i <= sale.getMaximumNumber(); i++) {
                    int i = sale.getNumber();
                    if (sale.getPresidentExchange() == 0) {
                        options.add(LocalText.getText("SellShares",
                                i,
                                sale.getShare(),
                                i * sale.getShare(),
                                sale.getCompanyName(),
                                Bank.format(i * sale.getShareUnits()
                                        * sale.getPrice()) ));
                    } else {
                        options.add(LocalText.getText("SellSharesWithSwap",
                                i,
                                sale.getShare(),
                                i * sale.getShare(),
                                sale.getCompanyName(),
                                Bank.format(i * sale.getShareUnits() * sale.getPrice()),
                                // disregard other than double pres.certs. This is for 1835 only.
                                3 - sale.getPresidentExchange(),
                                sale.getPresidentExchange() * sale.getShareUnit()));
                    }
                    sellActions.add(sale);
                    sellAmounts.add(i);
                }
                int index = 0;
                if (options.size() > 1) {
                    String message = LocalText.getText("PleaseSelect");
                    String sp =
                        (String) JOptionPane.showInputDialog(this, message,
                                message, JOptionPane.QUESTION_MESSAGE,
                                null, options.toArray(new String[0]),
                                options.get(0));
                    index = options.indexOf(sp);
                } else if (options.size() == 1) {
                    String message = LocalText.getText("PleaseConfirm");
                    int result =
                        JOptionPane.showConfirmDialog(this, options.get(0),
                                message, JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                    index = (result == JOptionPane.OK_OPTION ? 0 : -1);
                }
                if (index < 0) {
                    // cancelled
                } else {
                    chosenAction = sellActions.get(index);
                    //((SellShares) chosenAction).setNumberSold(sellAmounts.get(index));
                }
            } else if (actions.get(0) instanceof BuyCertificate) {
                boolean startCompany = false;

                List<String> options = new ArrayList<String>();
                List<BuyCertificate> buyActions =
                    new ArrayList<BuyCertificate>();
                List<Integer> buyAmounts = new ArrayList<Integer>();
                BuyCertificate buy;
                String companyName = "";
                String playerName = "";
                int sharePerCert;
                int sharesPerCert;
                int shareUnit;

                for (PossibleAction action : actions) {
                    buy = (BuyCertificate) action;
                    //cert = buy.getCertificate();
                    playerName = buy.getPlayerName ();
                    PublicCompanyI company = buy.getCompany();
                    companyName = company.getName();
                    sharePerCert = buy.getSharePerCertificate();
                    shareUnit = company.getShareUnit();
                    sharesPerCert = sharePerCert / shareUnit;

                    if (buy instanceof StartCompany) {

                        startCompany = true;
                        int[] startPrices;
                        if (((StartCompany_1880) buy).mustSelectAPrice()) {
                            startPrices =
                                ((StartCompany_1880) buy).getStartPrices();
                            List<ParSlot_1880> startParSlots = ((StartCompany_1880) buy).getStartParSlots();
                            Arrays.sort(startPrices);
                            if (startPrices.length > 1) {
                                for (ParSlot_1880 parSlot : startParSlots) {
                                    options.add(LocalText.getText("StartCompany",
                                            Bank.format(parSlot.getPrice()),
                                            sharePerCert,
                                            Bank.format(sharesPerCert * parSlot.getPrice()) )+" Slot - "+(parSlot.getIndex()+1));
                                    buyActions.add(buy);
                                    buyAmounts.add(parSlot.getPrice());
                                }
                            } else {
                                options.add (LocalText.getText("StartACompany",
                                        companyName,
                                        company.getPresidentsShare().getShare(),
                                        Bank.format(company.getPresidentsShare().getShares() * startPrices[0])));
                                buyActions.add(buy);
                                buyAmounts.add(startPrices[0]);
                            }
                        } else {
                            startPrices = new int[] {((StartCompany) buy).getPrice()};
                            options.add(LocalText.getText("StartCompanyFixed",
                                    companyName,
                                    sharePerCert,
                                    Bank.format(startPrices[0]) ));
                            buyActions.add(buy);
                            buyAmounts.add(startPrices[0]);
                        }

                    } else {

                        options.add(LocalText.getText("BuyCertificate",
                                sharePerCert,
                                companyName,
                                buy.getFromPortfolio().getName(),
                                Bank.format(sharesPerCert * buy.getPrice()) ));
                        buyActions.add(buy);
                        buyAmounts.add(1);
                        for (int i = 2; i <= buy.getMaximumNumber(); i++) {
                            options.add(LocalText.getText("BuyCertificates",
                                    i,
                                    sharePerCert,
                                    companyName,
                                    buy.getFromPortfolio().getName(),
                                    Bank.format(i * sharesPerCert
                                            * buy.getPrice()) ));
                            buyActions.add(buy);
                            buyAmounts.add(i);
                        }
                    }
                }
                int index = 0;
                if (options.size() > 1) {
                    if (startCompany) {
                        RadioButtonDialog dialog = new RadioButtonDialog (
                                GameUIManager.COMPANY_START_PRICE_DIALOG,
                                gameUIManager,
                                parent,
                                LocalText.getText("PleaseSelect"),
                                LocalText.getText("WHICH_START_PRICE",
                                        playerName,
                                        companyName),
                                        options.toArray(new String[0]), -1);
                        gameUIManager.setCurrentDialog(dialog, actions.get(0));
                        parent.disableButtons();
                        return;
                    } else {
                        String sp =
                            (String) JOptionPane.showInputDialog(this,
                                    LocalText.getText(startCompany
                                            ? "WHICH_PRICE"
                                                    : "HOW_MANY_SHARES"),
                                                    LocalText.getText("PleaseSelect"),
                                                    JOptionPane.QUESTION_MESSAGE, null,
                                                    options.toArray(new String[0]),
                                                    options.get(0));
                        index = options.indexOf(sp);
                    }
                } else if (options.size() == 1) {
                    if (startCompany) {
                        RadioButtonDialog dialog = new RadioButtonDialog (
                                GameUIManager.COMPANY_START_PRICE_DIALOG,
                                gameUIManager,
                                parent,
                                LocalText.getText("PleaseSelect"),
                                LocalText.getText("WHICH_START_PRICE",
                                        playerName,
                                        companyName),
                                        options.toArray(new String[0]), -1);
                        gameUIManager.setCurrentDialog(dialog, actions.get(0));
                        parent.disableButtons();
                        return;
                    } else {
                    int result =
                        JOptionPane.showConfirmDialog(this, options.get(0),
                                LocalText.getText("PleaseConfirm"),
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                    index = (result == JOptionPane.OK_OPTION ? 0 : -1);
                    }
                }
                if (index < 0) {
                    // cancelled
                } else if (startCompany) {
                    chosenAction = buyActions.get(index);
                    ((StartCompany) chosenAction).setStartPrice(buyAmounts.get(index));
                    ((StartCompany) chosenAction).setNumberBought(((StartCompany) chosenAction).getSharesPerCertificate());
                } else {
                    chosenAction = buyActions.get(index);
                    ((BuyCertificate) chosenAction).setNumberBought(buyAmounts.get(index));
                }
            } else if (actions.get(0) instanceof CashCorrectionAction) {
                CashCorrectionAction cca = (CashCorrectionAction)actions.get(0);
                String amountString = (String) JOptionPane.showInputDialog(this,
                        LocalText.getText("CorrectCashDialogMessage", cca.getCashHolderName()),
                        LocalText.getText("CorrectCashDialogTitle"),
                        JOptionPane.QUESTION_MESSAGE, null, null, 0);
                if (amountString.substring(0,1).equals("+"))
                    amountString = amountString.substring(1);
                int amount;
                try {
                    amount = Integer.parseInt(amountString);
                } catch (NumberFormatException e) {
                    amount = 0;
                }
                cca.setAmount(amount);
                chosenAction = cca;
            } else {

                chosenAction =
                    processGameSpecificActions(actor, actions.get(0));

            }
        } else {
            log.warn("Action from unknown source: " + source.toString());
        }

        chosenAction = processGameSpecificFollowUpActions(actor, chosenAction);

        if (chosenAction != null)
            (parent).process(chosenAction);

        repaint();

    }


}
