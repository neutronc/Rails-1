/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/Action.java,v 1.3 2006/07/22 22:51:53 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.action;

import game.GameManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Erik Vos
 */
public class Action {

    private List moves = new ArrayList();
    
    private static Action currentAction = null;
    private static Action lastAction = null;
    private static List actionStack = new ArrayList();
    
    private Action () {}
    
    public static boolean start () {
        //System.out.println(">>> Start Action");
        if (currentAction == null) {
            currentAction = new Action();
            return true;
        } else {
            System.out.println ("Action is already open");
            return false;
        }
    }
    
    public static boolean finish () {
        //System.out.println("<<< Finish Action");
        if (currentAction != null) {
            actionStack.add (currentAction);
            //currentAction.execute();
            lastAction = currentAction;
            currentAction = null;
            return true;
        } else {
            System.out.println ("No action open for finish");
            return false;
       }
    }
    
    public static boolean cancel () {
        if (currentAction != null) {
            currentAction = null;
            return true;
        } else {
            System.out.println ("No action open for cancel");
            return false;
         }
    }
    
    public static boolean add (Move move) {

        move.execute();
        if (currentAction != null) {
            currentAction.moves.add (0, move); // Prepare for undo in reverse order!
        	return true;
        } else {
            System.out.println ("No Action open for "+move);
            //new Exception ("No Action open for add: "+move).printStackTrace();
            
            return false;
        }
    }
    
    public static boolean undoLast () {
        if (lastAction != null && currentAction == null) {
            lastAction.undo();
            actionStack.remove(lastAction);
            if (actionStack.size() > 0) {
                lastAction = (Action) actionStack.get(actionStack.size()-1);
            } else {
                lastAction = null;
            }
            return true;
        } else {
            System.out.println ("Invalid undo");
            return false;
        }
    }
    
    public static boolean isEmpty() {
        return actionStack.size() == 0;
    }
    
    public static boolean isOpen() {
        return currentAction != null;
    }
    
    /**
     * Clear the whole stack.
     * To be used if a state change occurs that cannot (yet) be undone. 
     * @return
     */
    public static boolean clear () {
        if (currentAction != null) currentAction.execute();
        actionStack = new ArrayList();
        currentAction = lastAction = null;
        return true;
        
    }
    
    public void execute () {
        
        for (Iterator it = moves.iterator(); it.hasNext(); ) {
            ((Move)it.next()).execute();
        }
    }
    
    public void undo () {
        // TODO: Must actually do this in reverse order
        for (Iterator it = moves.iterator(); it.hasNext(); ) {
            ((Move)it.next()).undo();
        }
    }
}
