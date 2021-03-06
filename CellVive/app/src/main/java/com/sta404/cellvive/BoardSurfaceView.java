package com.sta404.cellvive;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.sta404.cellvive.activities.CellViveActivity;
import com.sta404.cellvive.activities.QuestionActivity;
import com.sta404.cellvive.cell.Cell;
import com.sta404.cellvive.cell.EnemyCell;
import com.sta404.cellvive.cell.FoodCell;
import com.sta404.cellvive.cell.PlayerCell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Name: BoardSurfaceView
 *
 * This class extends the SurfaceView class and allows
 * for more intensive graphic processing due to the it
 * not running on the UIThread.
 */

public class BoardSurfaceView extends SurfaceView implements Runnable{
    private SurfaceHolder holder;
    private Thread thread;

    private boolean isRunning = true;
    private Paint p;

    private ArrayList<Cell> cells = new ArrayList<Cell>();
    private ArrayList<Cell> newCellsToAdd = new ArrayList<Cell>();

    private PlayerCell playerCell;

    //Used by Cells to determine location
    private final Random rand = new Random();

    private final Handler createFoodHandler = new Handler();
    private final int foodDelay = 500;

    private final Handler createEnemyHandler = new Handler();
    private final int enemyDelay = 3000;

    private int screenWidth;
    private int screenHeight;

    //This is used to refer back to the activity.
    private CellViveActivity activity;

    public BoardSurfaceView(Context c) {
        super(c);

        //Used to get the screen height and width.
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        p = new Paint();
        p.setColor(Color.BLACK);

        holder = getHolder();
        thread = new Thread(this);
        thread.start();

        //Adds Food to the board
        createFoodHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                    newCellsToAdd.add(new FoodCell(rand.nextInt(screenWidth),rand.nextInt(screenHeight)));
                    createFoodHandler.postDelayed(this, foodDelay);
            }
        }, foodDelay);

        //Adds Enemies to the board
        createEnemyHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                    newCellsToAdd.add(new EnemyCell(rand.nextInt(screenWidth-100), rand.nextInt(screenHeight-100), 5, 5));
                    createEnemyHandler.postDelayed(this, enemyDelay);
            }
        }, enemyDelay);

        //Adds the initial Food cells
        for(int i = 0; i < 50 ; i++){
            cells.add(new FoodCell(rand.nextInt(screenWidth),rand.nextInt(screenHeight)));
        }

        //Centre's the player on screen
        playerCell = new PlayerCell((screenWidth/2), (screenHeight/2)-75);
        cells.add(playerCell);
    }

    /**
     * Overrides the Thread's run method
     * This method redraws the canvas as well as
     * collision detection of the cells.
     */
    @Override
    public void run() {
        while(isRunning){
            if(!holder.getSurface().isValid()){
                continue;
            }
            Canvas canvas = holder.lockCanvas();
            if(canvas != null) {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), p);
                for (Cell cell : cells) {
                    cell.move(canvas);
                    if(cell.getBounds().intersect(playerCell.getBounds())){
                        if(cell instanceof EnemyCell){
                            Intent intent = new Intent(activity, QuestionActivity.class);
                            activity.startActivityForResult(intent, 1);

                            cell.killCell();
                            cell.drawCell(canvas);
                            isRunning = false;
                        }
                        else if (cell instanceof FoodCell){
                            activity.updateScore(); //TODO change to eatFood which calls updateScore with a param (i.e. updateScore(1)
                            cell.killCell();
                            cell.drawCell(canvas);
                        }
                    }
                }
                removeDeadCells();
                holder.unlockCanvasAndPost(canvas);
                cells.addAll(newCellsToAdd);
                newCellsToAdd.clear();
            }
        }
    }

    /**
     * Removes any dead cells from the cells ArrayList
     * An iterator is used to ensure concurrency is kept
     * while iterating the cells arrayList on the separate thread.
     */
    public void removeDeadCells(){
        Iterator<Cell> i = cells.iterator();
        while(i.hasNext()){
            Cell c = i.next();
            if(!c.isAlive()){
                i.remove();
            }
        }
    }

    public void setActivity(CellViveActivity activity) {
        this.activity = activity;
    }

    /**
     * Starts the thread
     */
    public void start(){
        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Sets the isRunning field to the param passed in
     * @param state
     */
    public void setRunning(boolean state){
        isRunning = state;
    }

    /**
     * Sets the X Coordinate of the Player Cell
     * @param x
     */
    public void setNewXPlayerCell(float x){
        playerCell.setNewX(x);
    }

    /**
     * Sets the Y Coordinate of the Player Cell
     * @param y
     */
    public void setNewYPlayerCell(float y){
        playerCell.setNewY(y);
    }

    /**
     * Checks to see if the PlayerCell has
     * been instantiated
     * @return boolean exists
     */
    public boolean playerCellExists(){
        if(playerCell != null){
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Stops the thread.
     */
    public void stop(){
        isRunning = false;
        while(true){
            try {
                thread.join();
                break;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            break;
        }
    }
}
