package src.game;

import java.util.ArrayList;
import java.awt.Rectangle;

public class GameObject {
    public enum Type{
        solid,
        oneWay,
        background,
        spawnPoint,
        respawn,
        death,
        ladder,
        trigger,
        win
    }
    public enum movement{
        always,
        touch,
        trigger,
        never
    }
    public double lastX, lastY, deltaX, deltaY;
    public int x, y;
    public int width, height;
    public Type type;
    public int mapID;
    public int triggeredX, triggeredY, triggeredBy, activatesChannel;
    public boolean isTriggered, triggeredDisplay;
    public int time, timeElapsed;
    public int moveX, moveY;
    public movement move;
    public int triggerMoveX, triggerMoveY;
    public int currentX, currentY;

    public ArrayList<Integer> pathX = new ArrayList<>();
    public ArrayList<Integer> pathY = new ArrayList<>();
    public ArrayList<Double> pathSpeed = new ArrayList<>();
    public ArrayList<Boolean> pathSpeedIsSpeed = new ArrayList<>();

    public ArrayList<Integer> triggeredPathX = new ArrayList<>();
    public ArrayList<Integer> triggeredPathY = new ArrayList<>();
    public ArrayList<Double> triggeredPathSpeed = new ArrayList<>();
    public ArrayList<Boolean> triggeredPathSpeedIsSpeed = new ArrayList<>();
    
    public GameObject(int x, int y, int width, int height, Type type, int mapID, boolean isTriggered,
        int triggeredX, int triggeredY, boolean triggeredDisplay, int triggeredBy, int activatesChannel,
        int time, int timeElapsed, movement move, int moveX, int moveY, int triggerMoveX, int triggerMoveY, int currentX, int currentY,
        ArrayList<Integer> pathX, ArrayList<Integer> pathY, ArrayList<Double> pathSpeed, ArrayList<Boolean> pathSpeedIsSpeed,
        ArrayList<Integer> triggeredPathX, ArrayList<Integer> triggeredPathY, ArrayList<Double> triggeredPathSpeed, ArrayList<Boolean> triggeredPathSpeedIsSpeed){
        
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.mapID = mapID;
        this.isTriggered = isTriggered;
        this.triggeredX = triggeredX;
        this.triggeredY = triggeredY;
        this.triggeredDisplay = triggeredDisplay;
        this.triggeredBy = triggeredBy;
        this.activatesChannel = activatesChannel;
        this.time = time;
        this.timeElapsed = timeElapsed;
        this.move = move;
        this.moveX = moveX;
        this.moveY = moveY;
        this.triggerMoveX = triggerMoveX;
        this.triggerMoveY = triggerMoveY;
        this.currentX = currentX;
        this.currentY = currentY;
        this.lastX = x;
        this.lastY = y;
        this.deltaX = 0;
        this.deltaY = 0;

        if(pathX != null) this.pathX.addAll(pathX);
        if(pathY != null) this.pathY.addAll(pathY);
        if(pathSpeed != null) this.pathSpeed.addAll(pathSpeed);
        if(pathSpeedIsSpeed != null) this.pathSpeedIsSpeed.addAll(pathSpeedIsSpeed);

        if(triggeredPathX != null) this.triggeredPathX.addAll(triggeredPathX);
        if(triggeredPathY != null) this.triggeredPathY.addAll(triggeredPathY);
        if(triggeredPathSpeed != null) this.triggeredPathSpeed.addAll(triggeredPathSpeed);
        if(triggeredPathSpeedIsSpeed != null) this.triggeredPathSpeedIsSpeed.addAll(triggeredPathSpeedIsSpeed);
    }
    public GameObject(int x, int y, int width, int height, Type type, int mapID){
    this(x, y, width, height, type, mapID, false, x, y, true, 0, 0, 0, 0, GameObject.movement.never, x, y, x, y, x, y, 
        new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 
        new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
    public void updateMovement(double playerX, double playerY, int playerSize){
        this.lastX = this.currentX;
        this.lastY = this.currentY;
        if(this.move == movement.never){
            this.currentX = isTriggered ? (triggeredPathX.isEmpty() ? triggeredX : triggeredPathX.get(0)) : x;
            this.currentY = isTriggered ? (triggeredPathY.isEmpty() ? triggeredY : triggeredPathY.get(0)) : y;
            this.deltaX = this.currentX - this.lastX;
            this.deltaY = this.currentY - this.lastY;
            return;
        }


        ArrayList<Integer> pX = isTriggered ? triggeredPathX : pathX;
        ArrayList<Integer> pY = isTriggered ? triggeredPathY : pathY;
        ArrayList<Double> pSpd = isTriggered ? triggeredPathSpeed : pathSpeed;
        ArrayList<Boolean> pIsSpd = isTriggered ? triggeredPathSpeedIsSpeed : pathSpeedIsSpeed;

        ArrayList<PointD> points = new ArrayList<>();
        double startX = isTriggered ? (triggeredPathX.isEmpty() ? x : triggeredX) : x;
        double startY = isTriggered ? (triggeredPathY.isEmpty() ? y : triggeredY) : y;
        points.add(new PointD(startX, startY));

        if(pX != null){
            for(int i = 0; i < pX.size(); i++){
                points.add(new PointD(pX.get(i), pY.get(i)));
            }
        }

        if(points.size() <= 1){
            this.currentX = (int) points.get(0).x;
            this.currentY = (int)points.get(0).y;
            this.deltaX = this.currentX - this.lastX;
            this.deltaY = this.currentY - this.lastY;
            return;
        }

        ArrayList<Double> segmentDurations = new ArrayList<>();
        double totalDuration = 0;
        for(int i = 0; i < points.size(); i++){
            PointD p1 = points.get(i);
            PointD p2 = points.get((i+1) % points.size());
            double dist = Math.hypot(p2.x - p1.x, p2.y - p1.y);

            double speedVal = (pSpd != null && i < pSpd.size()) ? pSpd.get(i) : 2.0;
            boolean isSpeed = (pIsSpd != null && i < pIsSpd.size()) ? pIsSpd.get(i) : true;

            double duration;
            if(isSpeed){
                double spd = Math.max(0.1,speedVal);
                duration = dist / spd;
            }else{
                duration = Math.max(1.0, speedVal);
            }
            segmentDurations.add(duration);
            totalDuration += duration;
        }

        if(totalDuration <= 0) totalDuration = 1;

        timeElapsed++;
        double currentCycleTime = timeElapsed % totalDuration;

        double accumulated = 0;
        int activeSegment = 0;
        double segmentProgress = 0;

        for(int i = 0; i < segmentDurations.size(); i++){
            double dur = segmentDurations.get(i);
            if(currentCycleTime <= accumulated + dur){
                activeSegment = i;
                segmentProgress = (currentCycleTime - accumulated) / dur;
                break;
            }
            accumulated += dur;
        }
        
        PointD startPt = points.get(activeSegment);
        PointD endPt = points.get((activeSegment + 1) % points.size());

        this.currentX = (int) Math.round(startPt.x + (endPt.x - startPt.x) * segmentProgress);
        this.currentY = (int) Math.round(startPt.y + (endPt.y - startPt.y) * segmentProgress);

        this.deltaX = this.currentX - this.lastX;
        this.deltaY = this.currentY - this.lastY;

    }
    private static class PointD{
        public double x, y;
        public PointD(double x, double y){this.x = x; this.y = y;}
    }

}