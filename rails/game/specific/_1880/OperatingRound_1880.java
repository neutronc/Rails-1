/**
 * 
 */
package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rails.common.DisplayBuffer;
import rails.common.GuiDef;
import rails.common.LocalText;
import rails.game.Bank;
import rails.game.BaseToken;
import rails.game.CashHolder;
import rails.game.GameDef;
import rails.game.GameDef.OrStep;
import rails.game.GameManagerI;
import rails.game.MapHex;
import rails.game.OperatingRound;
import rails.game.PhaseI;
import rails.game.Player;
import rails.game.Portfolio;
import rails.game.PrivateCompanyI;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;
import rails.game.ReportBuffer;
import rails.game.Stop;
import rails.game.TileI;
import rails.game.TrainI;
import rails.game.TrainManager;
import rails.game.TrainType;
import rails.game.action.BuyTrain;
import rails.game.action.LayTile;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.SetDividend;
import rails.game.move.CashMove;
import rails.game.move.ObjectMove;
import rails.game.special.SpecialTileLay;
import rails.game.special.SpecialTrainBuy;
import rails.game.specific._1880.PublicCompany_1880;
import rails.game.specific._1880.GameManager_1880;
import rails.game.state.EnumState;
import rails.util.SequenceUtil;

/**
 * @author Martin
 * 
 */
public class OperatingRound_1880 extends OperatingRound {

    
    List<Investor_1880> investorsToClose = new ArrayList<Investor_1880>();
    
    /**
     * @param gameManager
     */
    public OperatingRound_1880(GameManagerI gameManager_1880) {
        super(gameManager_1880);
  }

    @Override
    public void processPhaseAction(String name, String value) {
        if (name.equalsIgnoreCase("RaisingCertAvailability")) {
            for (PublicCompanyI company : gameManager.getAllPublicCompanies()) {
                if (company instanceof PublicCompany_1880) {
                    ((PublicCompany_1880) company).setAllCertsAvail(true);
                    if (!company.hasFloated()) {
                        ((PublicCompany_1880) company).setFloatPercentage(30);
                    } else { 
                        ((PublicCompany_1880) company).setFullFundingAvail();
                    }
                }
            }
        }
        if (name.equalsIgnoreCase("CommunistTakeOver")) {
            for (PublicCompanyI company : getOperatingCompanies()) {
                if (((company instanceof PublicCompany_1880) && company.hasFloated())) {
                    ((PublicCompany_1880) company).setCommunistTakeOver(true);
                }
            }
            for (PublicCompanyI company : gameManager.getAllPublicCompanies()) {
                if (((company instanceof PublicCompany_1880) && !company.hasFloated())) {
                    ((PublicCompany_1880) company).setFloatPercentage(40);
                }
            }
        }
        if (name.equalsIgnoreCase("ShanghaiExchangeOpen")) {
            for (PublicCompanyI company : getOperatingCompanies()) {
                if (((company instanceof PublicCompany_1880) && company.hasFloated())) {
                    ((PublicCompany_1880) company).setCommunistTakeOver(false);
                }
            }
            for (PublicCompanyI company : gameManager.getAllPublicCompanies()) {
                if (((company instanceof PublicCompany_1880) && !company.hasFloated())) {
                    ((PublicCompany_1880) company).setFloatPercentage(60);
                }
            }
        }
    }

    @Override
    protected void prepareRevenueAndDividendAction() {
        int[] allowedRevenueActions = new int[] {};
        // There is only revenue if there are any trains
        if (operatingCompany.get().canRunTrains()) {
            if (operatingCompany.get().hasStockPrice()) {
                allowedRevenueActions =
                        new int[] { SetDividend.PAYOUT, SetDividend.WITHHOLD };
            } else { // Investors in 1880 are not allowed to hand out Cash except
                     // in Closing
                allowedRevenueActions = new int[] { SetDividend.WITHHOLD };
            }

            possibleActions.add(new SetDividend(
                    operatingCompany.get().getLastRevenue(), true,
                    allowedRevenueActions));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#initNormalTileLays()
     */
    @Override
    protected void initNormalTileLays() {
        /**
         * Create a List of allowed normal tile lays (see LayTile class). This
         * method should be called only once per company turn in an OR: at the
         * start of the tile laying step.
         */
        // duplicate the phase colours
        Map<String, Integer> newTileColours = new HashMap<String, Integer>();
        for (String colour : getCurrentPhase().getTileColours()) {
            int allowedNumber =
                    operatingCompany.get().getNumberOfTileLays(colour);
            // Replace the null map value with the allowed number of lays
            newTileColours.put(colour, new Integer(allowedNumber));
        }
        // store to state
        tileLaysPerColour.initFromMap(newTileColours);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#start()
     */
    @Override
    public void start() {
        thisOrNumber = gameManager.getORId();

        ReportBuffer.add(LocalText.getText("START_OR", thisOrNumber));

        for (Player player : gameManager.getPlayers()) {
            player.setWorthAtORStart();
        }

        privatesPayOut();

        if ((operatingCompanies.size() > 0)
            && (gameManager.getAbsoluteORNumber() >= 1)) {
            // even if the BCR is sold she doesn't operate until all privates
            // have been sold
            // the absolute OR value is not incremented if not the startpacket
            // has been sold completely

            StringBuilder msg = new StringBuilder();
            for (PublicCompanyI company : operatingCompanies.viewList()) {
                msg.append(",").append(company.getName());
            }
            if (msg.length() > 0) msg.deleteCharAt(0);
            log.info("Initial operating sequence is " + msg.toString());

            if (stepObject == null) {
                stepObject =
                        new EnumState<GameDef.OrStep>("ORStep",
                                GameDef.OrStep.INITIAL);
                stepObject.addObserver(this);
            }

            if (setNextOperatingCompany(true)) {
                setStep(((GameManager_1880) gameManager).getNextOperatingPhase());
            } else {
                ((GameManager_1880) gameManager).setFirstCompanyToOperate(null);
                ((GameManager_1880) gameManager).setSkipFirstCompanyToOperate(false);
                ((GameManager_1880) gameManager).setNextOperatingPhase(OrStep.INITIAL);
                finishOR();
                             }                     

            return;
        }

        // No operating companies yet: close the round.
        String text = LocalText.getText("ShortORExecuted");
        ReportBuffer.add(text);
        DisplayBuffer.add(text);
        finishRound();
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#buyTrain(rails.game.action.BuyTrain)
     */
    @Override
    public boolean buyTrain(BuyTrain action) {

        SpecialTrainBuy stb = null;
        PublicCompany_1880 oldLastTrainBuyingCompany = null;
        TrainManager TrainMgr = gameManager.getTrainManager();
        List<TrainI> trains;
        boolean lastTrainOfType = false;

        stb = action.getSpecialProperty();

        trains = TrainMgr.getAvailableNewTrains();

        if ((trains.size() == 1)
            && (ipo.getTrainsPerType(trains.get(0).getType()).length == 1)) {
            // Last available train of a type is on for grabs..
            lastTrainOfType = true;
        }

        if (stb != null) { // A special Train buying right that gets exercised
                           // doesnt accelerate the train rush

            oldLastTrainBuyingCompany =
                    ((GameManager_1880) gameManager).getLastTrainBuyingCompany();

            if (super.buyTrain(action)) {
                if (action.getFromPortfolio() == ipo) {
                    if (stb.isExercised()) {
                        ((GameManager_1880) gameManager).setLastTrainBuyingCompany(oldLastTrainBuyingCompany);
                    } else {
                        ((GameManager_1880) gameManager).setLastTrainBuyingCompany((PublicCompany_1880) operatingCompany.get());
                    }
                    // Check: Did we just buy the last Train of that Type ? Then we
                    // fire up the Stockround
                    if (lastTrainOfType) {
                    
                    //but we also possibly have to close the P0 and payout the owner
                    //depending on the train sold with either:
                    // 40 Yuan if the train was a 2+2
                    // 70 Yuan if the train was a 3
                    // 100 yuan if the train was a 3+3
                    //TODO : How can we fit this in the action Logic here ??
                        ((GameManager_1880) gameManager).setFirstCompanyToOperate(operatingCompany.get());
                        ((GameManager_1880) gameManager).setSkipFirstCompanyToOperate(false);
                        ((GameManager_1880) gameManager).setNextOperatingPhase(getStep());
                        finishOR();
                        return true;
                    }
                }
                return true;
            } else {
                return false;
            }
        } else {
            if (super.buyTrain(action)) {
                if (action.getFromPortfolio() == ipo) {
                ((GameManager_1880) gameManager).setLastTrainBuyingCompany((PublicCompany_1880) operatingCompany.get());
                // Check: Did we just buy the last Train of that Type ? Then we
                // fire up the Stockround
                if (lastTrainOfType) {
                        ((GameManager_1880) gameManager).setFirstCompanyToOperate(operatingCompany.get());
                        ((GameManager_1880) gameManager).setSkipFirstCompanyToOperate(false);
                        ((GameManager_1880) gameManager).setNextOperatingPhase(getStep());
                        finishOR();
                        return true;
                    }

                }
                return true;
            } else {
                return false;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#newPhaseChecks()
     */
    @Override
    protected void newPhaseChecks() {
        PhaseI newPhase = getCurrentPhase();
        if (newPhase.getName().equals("2+2")) {
            askForPrivateRocket(newPhase);
        }
        else if (newPhase.getName().equals("3")) {
            askForPrivateRocket(newPhase);
        }
        else if (newPhase.getName().equals("3+3")) {
            askForPrivateRocket(newPhase);
        }
        else if (newPhase.getName().equals("4")) {
            askForPrivateRocket(newPhase);
        }
        else if (newPhase.getName().equals("8")) {
            ((GameManager_1880) gameManager).numOfORs.set(2); 
            // After the first 8 has been bought there will be a last
            // Stockround and two ORs.
        } else if (newPhase.getName().equals("8e")) {
                return;
        }

    }

 
    @Override
    public void resume() {

        guiHints.setActivePanel(GuiDef.Panel.MAP);
        guiHints.setCurrentRoundType(getClass());

        if (getOperatingCompany() != null) {
            setStep(GameDef.OrStep.BUY_TRAIN);
        } else {
            ((GameManager_1880) gameManager).setFirstCompanyToOperate(null);
            ((GameManager_1880) gameManager).setSkipFirstCompanyToOperate(false);
            ((GameManager_1880) gameManager).setNextOperatingPhase(OrStep.INITIAL);
            finishOR();
        }
        wasInterrupted.set(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#process(rails.game.action.PossibleAction)
     */
    @Override
    public boolean process(PossibleAction action) {

        boolean result = false;

        selectedAction = action;

        if (selectedAction instanceof NullAction) {
            NullAction nullAction = (NullAction) action;
            switch (nullAction.getMode()) {
            case NullAction.DONE: // Making Sure that the NullAction.DONE is in
                                  // the Buy_Train Step..
                if (getStep() != GameDef.OrStep.BUY_TRAIN) {
                    result = done();
                    break;
                }
                if (operatingCompany.get() == ((GameManager_1880) gameManager).getLastTrainBuyingCompany()) {
                    if ((trainsBoughtThisTurn.isEmpty())
                        && (wasInterrupted.booleanValue() == false)) {
                        
                        // The current Company is the Company that has bought
                        // the last train and that purchase was not in this OR..
                        // we now discard the remaining active trains of that
                        // Subphase and start a stockround...
                        List<TrainI> trains =
                                trainManager.getAvailableNewTrains();
                        TrainType activeTrainTypeToDiscard =
                                trains.get(0).getType();
                        TrainI[] trainsToDiscard =
                                bank.getIpo().getTrainsPerType(
                                        activeTrainTypeToDiscard);
                        for (TrainI train : trainsToDiscard) {
                            new ObjectMove(train, ipo, scrapHeap);
                        }
                        // Need to make next train available !
                        trainManager.checkTrainAvailability(trainsToDiscard[0],
                                ipo);
                        ((GameManager_1880) gameManager).setFirstCompanyToOperate(operatingCompany.get());
                        ((GameManager_1880) gameManager).setSkipFirstCompanyToOperate(true);
                        ((GameManager_1880) gameManager).setNextOperatingPhase(OrStep.INITIAL);
                        finishOR();
                        return true;
                    }
                }
                result = done();
                break;
            case NullAction.PASS:
                result = done();
                break;
            case NullAction.SKIP:
                skip();
                result = true;
                break;
            }
            return result;
        } else if (action instanceof CloseInvestor_1880) {
            closeInvestor(action);
            result = done();
            return result;
        } else {
            return super.process(action);
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#setPossibleActions()
     */
    @Override
    public boolean setPossibleActions() {

        /*
         * Filter out the Tile Lay Step if the operating Company is not allowed
         * to build anymore because it doesnt possess the necessary building
         * right for this phase. Only Majors need to be prechecked.
         */
        if ((getStep() == GameDef.OrStep.INITIAL)
            && (operatingCompany.get().getTypeName().equals("Major"))) {
            initTurn();
            if ((noMapMode)
                || (!((PublicCompany_1880) operatingCompany.get()).hasBuildingRightForPhase(gameManager.getCurrentPhase()))) {
                nextStep(GameDef.OrStep.LAY_TRACK);
            } else {
                initNormalTileLays(); // new: only called once per turn ?
                setStep(GameDef.OrStep.LAY_TRACK);
            }
        }

        if ((getStep() == GameDef.OrStep.BUY_TRAIN)
            && (operatingCompany.get() instanceof Investor_1880)) {
            setStep(GameDef.OrStep.TRADE_SHARES);
        }
        
        if ((getStep() == GameDef.OrStep.TRADE_SHARES) && (operatingCompany.get() instanceof Investor_1880)) {
            Investor_1880 investor = (Investor_1880) operatingCompany.get();
            if (investor.isConnectedToLinkedCompany() == true) {
                possibleActions.add(new CloseInvestor_1880(investor));
                return true;
            } else {
                nextStep(GameDef.OrStep.FINAL);
            }
        }
        
        return super.setPossibleActions();
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#payout(int)
     */
    @Override
    public void payout(int amount) {
        if (amount == 0) return;

        int part;
        int shares;

        Map<CashHolder, Integer> sharesPerRecipient = countSharesPerRecipient();

        // Calculate, round up, report and add the cash

        // Define a precise sequence for the reporting
        Set<CashHolder> recipientSet = sharesPerRecipient.keySet();
        for (CashHolder recipient : SequenceUtil.sortCashHolders(recipientSet)) {
            if (recipient instanceof Bank) continue;
            if (recipient instanceof Investor_1880) continue;
            shares = (sharesPerRecipient.get(recipient));
            if (shares == 0) continue;
            part =
                    (int) Math.ceil(amount * shares
                                    * operatingCompany.get().getShareUnit()
                                    / 100.0);
            ReportBuffer.add(LocalText.getText("Payout", recipient.getName(),
                    Bank.format(part), shares,
                    operatingCompany.get().getShareUnit()));
            pay(bank, recipient, part);
        }

        // Move the token
        operatingCompany.get().payout(amount);

    }
    
    private void closeInvestor(PossibleAction action) {
        CloseInvestor_1880 closeInvestorAction = (CloseInvestor_1880) action;
        Investor_1880 investor = closeInvestorAction.getInvestor();
        Player investorOwner = investor.getPresident();
        PublicCompany_1880 linkedCompany = (PublicCompany_1880) investor.getLinkedCompany();
        ReportBuffer.add(LocalText.getText("FIConnected", investor.getName(),
                  linkedCompany.getName()));
        
        // The owner gets $50
        ReportBuffer.add(LocalText.getText("FIConnectedPayout", investorOwner.getName()));
        new CashMove(bank, investorOwner, 50);
                
        // Pick where the treasury goes
        if (closeInvestorAction.getTreasuryToLinkedCompany() == true) {
        ReportBuffer.add(LocalText.getText("FIConnectedTreasuryToCompany", linkedCompany.getName(), investor.getName(), investor.getCash()));            
        new CashMove(investor, linkedCompany, investor.getCash());
        } else {
             ReportBuffer.add(LocalText.getText("FIConnectedTreasuryToOwner", investorOwner.getName(), investor.getName(), (investor.getCash()/5)));            
             new CashMove(investor, investorOwner, (investor.getCash() / 5));
             new CashMove(investor, bank, investor.getCash());
        }
                
        BaseToken token = (BaseToken) investor.getTokens().get(0);
        MapHex hex = investor.getHomeHexes().get(0);
        Stop city = (Stop) token.getHolder();
        token.moveTo(token.getCompany());
                
        // Pick if the token gets replaced
        if (closeInvestorAction.getReplaceToken() == true) {
           if (hex.layBaseToken(linkedCompany, city.getNumber())) {
               ReportBuffer.add(LocalText.getText("FIConnectedReplaceToken", linkedCompany.getName(), investor.getName()));            
               linkedCompany.layBaseToken(hex, 0); // (should this be city.getNumber as well?)
               }            
        } else {
               ReportBuffer.add(LocalText.getText("FIConnectedDontReplaceToken", linkedCompany.getName(), investor.getName()));            
        }
     // Move the certificate
         ReportBuffer.add(LocalText.getText("FIConnectedMoveCert", investorOwner.getName(), linkedCompany.getName(), investor.getName()));            
         Portfolio investorPortfolio = investor.getPortfolio();
         List<PublicCertificateI> investorCerts = investorPortfolio.getCertificates();
         investorCerts.get(0).moveTo(investorOwner.getPortfolio()); // should only be one
     
         // Set the company to close at the end of the operating round.  It's just too 
         // hard to do it immediately - the checks to see if the operating order changed 
         // conflict with the check to see if something closed.
        
         investorsToClose.add(investor);
     }

    /* (non-Javadoc)
     * @see rails.game.Round#setOperatingCompanies()
     */
    @Override
    public List<PublicCompanyI> setOperatingCompanies() {
        List<PublicCompanyI> companyList = new ArrayList<PublicCompanyI>();
      int key = 1;

        // Put in Foreign Investors first      
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
            if ((company instanceof Investor_1880) && (company.isClosed() == false)) {
                companyList.add(company);
            }
        }

      
        // Now the share companies in par slot order
        List<PublicCompanyI> companies = ((GameManager_1880) gameManager).getParSlots().getCompaniesInOperatingOrder();
        for (PublicCompanyI company : companies) {
            if (!canCompanyOperateThisRound(company)) continue; 
            if (!company.hasFloated()) continue;
            companyList.add(company);
        }
      
        // Skip ahead if we have to
        PublicCompanyI firstCompany = ((GameManager_1880) gameManager).getFirstCompanyToOperate();
        if (firstCompany != null) {
            while (companyList.get(0) != firstCompany) {
                companyList.remove(0);
            }
        }
        
        if (((GameManager_1880) gameManager).getSkipFirstCompanyToOperate() == true) {
            companyList.remove(0);
        }
               

        return new ArrayList<PublicCompanyI>(companyList);
    }

    /* (non-Javadoc)
     * @see rails.game.Round#setOperatingCompanies(java.util.List, rails.game.PublicCompanyI)
     */
    @Override
    public List<PublicCompanyI> setOperatingCompanies(
            List<PublicCompanyI> oldOperatingCompanies,
            PublicCompanyI lastOperatingCompany) {
        // TODO Auto-generated method stub
       return setOperatingCompanies();
    }

    /*=======================================
     *  4.   LAYING TILES
     *=======================================*/

    @Override
    public boolean layTile(LayTile action) {

        String errMsg = null;
        int cost = 0;
        SpecialTileLay stl = null;
        boolean extra = false;

        PublicCompanyI company = action.getCompany();
        String companyName = company.getName();
        TileI tile = action.getLaidTile();
        MapHex hex = action.getChosenHex();
        int orientation = action.getOrientation();

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.get().getName())) {
                errMsg =
                    LocalText.getText("WrongCompany",
                            companyName,
                            operatingCompany.get().getName() );
                break;
            }
            // Must be correct step
            if (getStep() != GameDef.OrStep.LAY_TRACK) {
                errMsg = LocalText.getText("WrongActionNoTileLay");
                break;
            }

            if (tile == null) break;

            if (!getCurrentPhase().isTileColourAllowed(tile.getColourName())) {
                errMsg =
                    LocalText.getText("TileNotYetAvailable",
                            tile.getExternalId());
                break;
            }
            if (tile.countFreeTiles() == 0) {
                errMsg =
                    LocalText.getText("TileNotAvailable",
                            tile.getExternalId());
                break;
            }

            /*
             * Check if the current tile is allowed via the LayTile allowance.
             * (currently the set if tiles is always null, which means that this
             * check is redundant. This may change in the future.
             */
            if (action != null) {
                List<TileI> tiles = action.getTiles();
                if (tiles != null && !tiles.isEmpty() && !tiles.contains(tile)) {
                    errMsg =
                        LocalText.getText(
                                "TileMayNotBeLaidInHex",
                                tile.getExternalId(),
                                hex.getName() );
                    break;
                }
                stl = action.getSpecialProperty();
                if (stl != null) extra = stl.isExtra();
            }

            /*
             * If this counts as a normal tile lay, check if the allowed number
             * of normal tile lays is not exceeded.
             */
            if (!extra && !validateNormalTileLay(tile)) {
                errMsg =
                    LocalText.getText("NumberOfNormalTileLaysExceeded",
                            tile.getColourName());
                break;
            }

            // Sort out cost
            if (stl != null && stl.isFree()) {
                cost = hex.getTileCost()-20;  //Or we implement a general SpecialRight that deduces cost if a private is owned
            } else {
                cost = hex.getTileCost();
            }

            // Amount must be non-negative multiple of 10
            if (cost < 0) {
                errMsg =
                    LocalText.getText("NegativeAmountNotAllowed",
                            Bank.format(cost));
                break;
            }
            if (cost % 10 != 0) {
                errMsg =
                    LocalText.getText("AmountMustBeMultipleOf10",
                            Bank.format(cost));
                break;
            }
            // Does the company have the money?
            if (cost > operatingCompany.get().getCash()) {
                errMsg =
                    LocalText.getText("NotEnoughMoney",
                            companyName,
                            Bank.format(operatingCompany.get().getCash()),
                            Bank.format(cost) );
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotLayTileOn",
                    companyName,
                    tile.getExternalId(),
                    hex.getName(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);

        if (tile != null) {
            if (cost > 0)
                new CashMove(operatingCompany.get(), bank, cost);
            operatingCompany.get().layTile(hex, tile, orientation, cost);

            if (cost == 0) {
                ReportBuffer.add(LocalText.getText("LaysTileAt",
                        companyName,
                        tile.getExternalId(),
                        hex.getName(),
                        hex.getOrientationName(orientation)));
            } else {
                ReportBuffer.add(LocalText.getText("LaysTileAtFor",
                        companyName,
                        tile.getExternalId(),
                        hex.getName(),
                        hex.getOrientationName(orientation),
                        Bank.format(cost) ));
            }
            hex.upgrade(action);

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                //currentSpecialTileLays.remove(action);
                log.debug("This was a special tile lay, "
                        + (extra ? "" : " not") + " extra");

            }
            if (!extra) {
                log.debug("This was a normal tile lay");
                registerNormalTileLay(tile);
            }
        }

        if (tile == null || !areTileLaysPossible()) {
            nextStep();
        }

        return true;
    }

    private void askForPrivateRocket(PhaseI newPhase) {
         
    }

    /* (non-Javadoc)
     * @see rails.game.OperatingRound#processGameSpecificAction(rails.game.action.PossibleAction)
     */
    @Override
    public boolean processGameSpecificAction(PossibleAction action) {
        return super.processGameSpecificAction(action);
    }

    /* (non-Javadoc)
     * @see rails.game.OperatingRound#setGameSpecificPossibleActions()
     */
    @Override
    protected void setGameSpecificPossibleActions() {
        super.setGameSpecificPossibleActions();
    }

    protected void finishOR() {
        for (Investor_1880 investor : investorsToClose) {
            investor.setClosed();
        }
        super.finishOR();
    }
    
    protected void finishTurn() {
        if (!operatingCompany.get().isClosed()) {
            operatingCompany.get().setOperated();
            companiesOperatedThisRound.add(operatingCompany.get());

            // Check if any privates must be closed (now only applies to 1856 W&SR)
            // Copy list first to avoid concurrent modifications
            for (PrivateCompanyI priv :
                new ArrayList<PrivateCompanyI> (operatingCompany.get().getPortfolio().getPrivateCompanies())) {
                priv.checkClosingIfExercised(true);
            }
        }

        if (!finishTurnSpecials()) return;

        if (setNextOperatingCompany(false)) {
            setStep(GameDef.OrStep.INITIAL);
        } else {
            ((GameManager_1880) gameManager).setFirstCompanyToOperate(null);
            ((GameManager_1880) gameManager).setSkipFirstCompanyToOperate(false);
            ((GameManager_1880) gameManager).setNextOperatingPhase(OrStep.INITIAL);
            finishOR();
        }
    }
    
    @Override
    protected void privatesPayOut() {
        if (((GameManager_1880) gameManager).getFirstCompanyToOperate() == null) {
            super.privatesPayOut();
       }
    }
    

    
}
