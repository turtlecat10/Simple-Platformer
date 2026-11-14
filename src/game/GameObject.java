package src.game;

import java.util.ArrayList;
import java.awt.Rectangle;
import java.awt.Point;

public class GameObject {
    public enum Type{
        solid,
        oneWay,
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
    public double lastX, lastY, deltaX = 0, deltaY = 0;
    public int x, y;
    public int width, height;
    public Type type;
    public int mapID;
    public int triggeredX, triggeredY, triggeredBy = 0, activatesChannel = 0;
    public boolean isTriggered = false, triggeredDisplay;
    public int time, timeElapsed = 0;
    public int moveX, moveY;
    public movement move;
    public boolean returnReverse;
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

    public boolean initializedPathTracking = false;
    public int currentSegmentIndex = 0;
    public double segmentProgress = 0.0;
    public boolean movingBackward = false;
    public boolean isReturningFromTouch = false;
    
    public GameObject(int x, int y, int width, int height, Type type, int mapID, boolean isTriggered,
        int triggeredX, int triggeredY, boolean triggeredDisplay, int triggeredBy, int activatesChannel,
        int time, int timeElapsed, movement move, boolean returnReverse, int moveX, int moveY, int triggerMoveX, int triggerMoveY, int currentX, int currentY,
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
        this.returnReverse = returnReverse;
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
    
    public GameObject(int x, int y, int width, int height, Type type, int mapID) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.mapID = mapID;
        this.triggeredX = x;
        this.triggeredY = y;
        this.move = movement.never;
        this.moveX = x;
        this.moveY = y;
        this.triggerMoveX = x;
        this.triggerMoveY = y;
        this.currentX = x;
        this.currentY = y;
        this.lastX = x;
        this.lastY = y;
    }
    
    public void updateMovement(double playerX, double playerY, int playerSize){
        this.lastX = this.currentX;
        this.lastY = this.currentY;

        if(this.move == movement.never){
            this.deltaX = 0;
            this.deltaY = 0;
            return;
        }

        if(this.move == movement.trigger && !this.isTriggered){
            this.currentX = this.x;
            this.currentY = this.y;
            this.deltaX = this.currentX - this.lastX;
            this.deltaY = this.currentY - this.lastY;
            this.initializedPathTracking = false;
            return;
        }

        ArrayList<Integer> pX = isTriggered ? triggeredPathX : pathX;
        ArrayList<Integer> pY = isTriggered ? triggeredPathY : pathY;
        ArrayList<Double> pSpd = isTriggered ? triggeredPathSpeed : pathSpeed;
        ArrayList<Boolean> pIsSpd = isTriggered ? triggeredPathSpeedIsSpeed : pathSpeedIsSpeed;

        ArrayList<PointD> points = new ArrayList<>();
        double startX = this.x;
        double startY = this.y;

        if(this.isTriggered){
            double trigX = (triggeredX == -1) ? x : triggeredX;
            double trigY = (triggeredY == -1) ? y : triggeredY;
            points.add(new PointD(this.currentX, this.currentY));
            points.add(new PointD(trigX, trigY));
        }else{
            points.add(new PointD(startX, startY));
        }

        if(pX != null && !pX.isEmpty()){
            for(int i = 0; i < pX.size(); i++){
                points.add(new PointD(pX.get(i), pY.get(i)));
            }
        }

        if(!this.returnReverse && points.size() > 1){
            double trigX = (triggeredX == -1) ? x : triggeredX;
            double trigY = (triggeredY == -1) ? y : triggeredY;
            PointD returnTarget = this.isTriggered ? new PointD(trigX, trigY) : new PointD(startX, startY);
            PointD lastPt = points.get(points.size() - 1);
            if(lastPt.x != returnTarget.x || lastPt.y != returnTarget.y){
                points.add(returnTarget);
            }
        }

        if(points.size() <= 1){
            this.currentX = (int) points.get(0).x;
            this.currentY = (int) points.get(0).y;
            this.deltaX = this.currentX - this.lastX;
            this.deltaY = this.currentY - this.lastY;
            return;
        }

        boolean isTouchingPlayer = (playerX + playerSize >= this.currentX && playerX <= this.currentX + this.width && playerY + playerSize >= (this.currentY - this.height) && playerY <= this.currentY);

        if(this.move == movement.touch){
            if(isTouchingPlayer){
                this.isReturningFromTouch = true;
            }
            if(!isTouchingPlayer && !this.isReturningFromTouch){
                this.deltaX = 0;
                this.deltaY = 0;
                return;
            }
        }

        if(!this.initializedPathTracking){
            this.currentSegmentIndex = 0;
            this.segmentProgress = 0.0;
            this.movingBackward = false;
            this.initializedPathTracking = true;
        }

        double direction = 1.0;
        if(this.move == movement.touch){
            if(isTouchingPlayer){
                direction = 1.0;
            }else{
                direction = -1.0;
            }
        }else if(this.movingBackward){
            direction = -1.0;
        }

        int maxSegIdx = points.size() - 2;
        if(this.currentSegmentIndex > maxSegIdx) this.currentSegmentIndex = maxSegIdx;
        if(this.currentSegmentIndex < 0) this.currentSegmentIndex = 0;

        PointD p1 = points.get(this.currentSegmentIndex);
        PointD p2 = points.get(this.currentSegmentIndex + 1);
        
        double distance = Math.hypot(p2.x - p1.x, p2.y - p1.y);
        double step = 2.0;
        
        if(pSpd != null && this.currentSegmentIndex < pSpd.size()){
            double val = pSpd.get(this.currentSegmentIndex);
            boolean isSpeed = (pIsSpd == null || this.currentSegmentIndex >= pIsSpd.size() || pIsSpd.get(this.currentSegmentIndex));
            if(isSpeed){
                step = val;
            }else{
                step = (val > 0) ? (distance / val) : distance;
            }
        }

        double increment = (distance > 0) ? (step / distance) : 1.0;
        this.segmentProgress += increment * direction;

        if(this.segmentProgress >= 1.0){
            if(this.currentSegmentIndex >= maxSegIdx){
                if(this.returnReverse){
                    this.movingBackward = true;
                    this.segmentProgress = 1.0;
                    this.currentSegmentIndex = maxSegIdx;
                }else{
                    this.currentSegmentIndex = 0;
                    this.segmentProgress = 0.0;
                }
            }else{
                if(!this.movingBackward){
                    this.currentSegmentIndex++;
                    this.segmentProgress = 0.0;
                }
            }
        }else if(this.segmentProgress <= 0.0){
            if(this.currentSegmentIndex <= 0){
                this.segmentProgress = 0.0;
                this.movingBackward = false;
                if(this.move == movement.touch){
                    this.isReturningFromTouch = false;
                }
            }else{
                this.currentSegmentIndex--;
                this.segmentProgress = 1.0;
            }
        }

        int segIdx = Math.min(this.currentSegmentIndex, maxSegIdx);
        p1 = points.get(segIdx);
        p2 = points.get(segIdx + 1);
        double prog = Math.max(0.0, Math.min(1.0, this.segmentProgress));

        this.currentX = (int) Math.round(p1.x + (p2.x - p1.x) * prog);
        this.currentY = (int) Math.round(p1.y + (p2.y - p1.y) * prog);

        this.deltaX = this.currentX - this.lastX;
        this.deltaY = this.currentY - this.lastY;
    }
    
    public void updateMovement1(double playerX, double playerY, int playerSize){
        if(this.move == movement.never){
            this.deltaX = 0;
            this.deltaY = 0;
            return;
        }
        this.lastX = this.currentX;
        this.lastY = this.currentY;


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
        if(this.move == movement.trigger && !this.isTriggered){
            return;
        }

        if(points.size() <= 1){
            this.currentX = (int) points.get(0).x;
            this.currentY = (int)points.get(0).y;
            this.deltaX = this.currentX - this.lastX;
            this.deltaY = this.currentY - this.lastY;
            return;
        }

        boolean isTouchingPlayer = (playerX + playerSize >= this.currentX && playerX <= this.currentX + this.width &&
                                playerY + playerSize >= (this.currentY - this.height) && playerY <= this.currentY); 

        if(this.move == movement.touch){
            if(!isTouchingPlayer && !isReturningFromTouch){
                return;
            }
        }

        if(!this.initializedPathTracking){
            this.currentSegmentIndex = 0;
            this.segmentProgress = 0.0;
            this.movingBackward = false;
            this.initializedPathTracking = true;
        }


        double direction = 1.0;
        if(this.move == movement.touch){
            if(isTouchingPlayer){
                direction = 1.0;
            }else{
                direction = -1.0;
            }
        }

        int segIdx = Math.min(this.currentSegmentIndex, points.size() - 2);
        if(segIdx < 0) segIdx = 0;

        PointD p1 = points.get(segIdx);
        PointD p2 = points.get(segIdx + 1);

        double distance = Math.hypot(p2.x - p1.x, p2.y - p1.y);
        double step = 2.0;
        
        if(pSpd != null && segIdx < pSpd.size()){
            double val = pSpd.get(segIdx);
            boolean isSpeed = (pIsSpd == null || segIdx >= pIsSpd.size() || pIsSpd.get(segIdx));
            if(isSpeed){
                step = val;
            }else{
                step = (val > 0) ? (distance / val) : distance;
            }
        }

        double increment = (distance > 0) ? (step / distance) : 1.0;
        this.segmentProgress += increment * direction;

        if(this.segmentProgress >= 1.0){
            if(segIdx >= points.size() - 2){
                if(this.returnReverse){
                    this.movingBackward = true;
                    this.segmentProgress = 1.0;
                    this.currentSegmentIndex = points.size() - 2;
                }else{
                    this.currentSegmentIndex = 0;
                    this.segmentProgress = 0.0;
                }
            }else{
                if(!this.movingBackward){
                    this.currentSegmentIndex++;
                    this.segmentProgress = 0.0;
                }
            }
        }else if(this.segmentProgress <= 0.0){
            if(this.currentSegmentIndex <= 0){
                this.segmentProgress = 0.0;
                this.movingBackward = false;
            }else{
                if(this.movingBackward){
                    this.currentSegmentIndex--;
                    this.segmentProgress = 1.0;
                }
            }
        }

        if(this.movingBackward && this.move != movement.touch){
            segIdx = Math.min(this.currentSegmentIndex, points.size() - 2);
            p1 = points.get(segIdx + 1);
            p2 = points.get(segIdx);
            double revProgress = 1.0 - this.segmentProgress;

            this.lastX = this.currentX;
            this.lastY = this.currentY;
            this.currentX = (int) (p1.x + (p2.x - p1.x) * revProgress);
            this.currentY = (int) (p1.y + (p2.y - p2.y) * revProgress);
        }else{
            segIdx = Math.min(this.currentSegmentIndex, points.size() - 2);
            p1 = points.get(segIdx);
            p2 = points.get(segIdx + 1);
            double prog = Math.max(0.0, Math.min(1.0, this.segmentProgress));

            this.lastX = this.currentX;
            this.lastY = this.currentY;
            this.currentX = (int) (p1.x + (p2.x - p1.x) * prog);
            this.currentY = (int) (p1.y + (p2.y - p1.y) * prog);
        }




        // ArrayList<Double> segmentDurations = new ArrayList<>();
        // double totalDuration = 0;
        // for(int i = 0; i < points.size(); i++){
        //     PointD p1 = points.get(i);
        //     PointD p2 = points.get((i+1) % points.size());
        //     double dist = Math.hypot(p2.x - p1.x, p2.y - p1.y);

        //     double speedVal = (pSpd != null && i < pSpd.size()) ? pSpd.get(i) : 2.0;
        //     boolean isSpeed = (pIsSpd != null && i < pIsSpd.size()) ? pIsSpd.get(i) : true;

        //     double duration;
        //     if(isSpeed){
        //         double spd = Math.max(0.1,speedVal);
        //         duration = dist / spd;
        //     }else{
        //         duration = Math.max(1.0, speedVal);
        //     }
        //     segmentDurations.add(duration);
        //     totalDuration += duration;
        // }

        // if(totalDuration <= 0) totalDuration = 1;

        // timeElapsed++;
        // double currentCycleTime = timeElapsed % totalDuration;

        // double accumulated = 0;
        // int activeSegment = 0;
        // double segmentProgress = 0;

        // for(int i = 0; i < segmentDurations.size(); i++){
        //     double dur = segmentDurations.get(i);
        //     if(currentCycleTime <= accumulated + dur){
        //         activeSegment = i;
        //         segmentProgress = (currentCycleTime - accumulated) / dur;
        //         break;
        //     }
        //     accumulated += dur;
        // }
        
        // PointD startPt = points.get(activeSegment);
        // PointD endPt = points.get((activeSegment + 1) % points.size());

        // this.currentX = (int) Math.round(startPt.x + (endPt.x - startPt.x) * segmentProgress);
        // this.currentY = (int) Math.round(startPt.y + (endPt.y - startPt.y) * segmentProgress);

        this.deltaX = this.currentX - this.lastX;
        this.deltaY = this.currentY - this.lastY;

    }
    
    private static class PointD{
        public double x, y;
        public PointD(double x, double y){this.x = x; this.y = y;}
    }
    
    public void resetToStart(){
        this.timeElapsed = 0;
        this.deltaX = 0;
        this.deltaY = 0;
        this.currentX = this.x;
        this.currentY = this.y;
        this.lastX = this.x;
        this.lastY = this.y;

        this.initializedPathTracking = false;
        this.currentSegmentIndex = 0;
        this.segmentProgress = 0.0;
        this.movingBackward = false;
        this.isReturningFromTouch = false;
    }
}