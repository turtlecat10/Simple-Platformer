package src.game;
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
    
    public GameObject(int x, int y, int width, int height, Type type, int mapID, boolean isTriggered, 
        int triggeredX, int triggeredY, boolean triggeredDisplay, int triggeredBy, int activatesChannel,
         int time, int timeElapsed, movement move, int moveX, int moveY, int triggerMoveX, int triggerMoveY, int currentX, int currentY){
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
    }
    public GameObject(int x, int y, int width, int height, Type type, int mapID){
        this(x, y, width, height, type, mapID, false, x, y, true, 0, 0, 0, 0, GameObject.movement.never, x, y, x, y, x, y);
    }
    public GameObject(int x, int y, int width, int height, Type type, int mapID, boolean isTriggered, int triggeredX, int triggeredY, boolean triggeredDisplay, 
        int triggeredBy, int activatesChannel){
        this(x, y, width, height, type, mapID, isTriggered, triggeredX, triggeredY, triggeredDisplay, triggeredBy, activatesChannel, 
            0, 0, GameObject.movement.never, x, y, triggeredX, triggeredY, x, y);
    }
    public void updateMovement(){
        this.lastX = this.currentX;
        this.lastY = this.currentY;
        if(this.move == movement.never){
            this.currentX = isTriggered ? triggeredX : x;
            this.currentY = isTriggered ? triggeredY : y;
            this.deltaX = this.currentX - this.lastX;
            this.deltaY = this.currentY - this.lastY;
            return;
        }
        int startPointX, endPointX;
        int startPointY, endPointY;

        int travelTime = Math.max(1, this.time);

        if(this.move == movement.always){
            if (this.isTriggered) {
                // STATE B: Trigger is ACTIVE. Switch immediately to loop 
                // between triggeredX/Y and triggerMoveX/Y!
                timeElapsed++;
                int fullCycle = travelTime * 4;
                int phase = timeElapsed % fullCycle;

                if (phase < travelTime) {
                    double t = (double) phase / travelTime;
                    this.currentX = (int) Math.round(triggeredX + (triggerMoveX - triggeredX) * t);
                    this.currentY = (int) Math.round(triggeredY + (triggerMoveY - triggeredY) * t);
                } else if (phase < travelTime * 2) {
                    this.currentX = triggerMoveX;
                    this.currentY = triggerMoveY;
                } else if (phase < travelTime * 3) {
                    double t = (double) (phase - (travelTime * 2)) / travelTime;
                    this.currentX = (int) Math.round(triggerMoveX + (triggeredX - triggerMoveX) * t);
                    this.currentY = (int) Math.round(triggerMoveY + (triggeredY - triggerMoveY) * t);
                } else {
                    this.currentX = triggeredX;
                    this.currentY = triggeredY;
                }
                
            } else {
                // STATE A: Default state / Trigger is INACTIVE. 
                // Continuously run standard oscillation between x/y and moveX/Y!
                timeElapsed++;
                int fullCycle = travelTime * 4;
                int phase = timeElapsed % fullCycle;

                if (phase < travelTime) {
                    double t = (double) phase / travelTime;
                    this.currentX = (int) Math.round(x + (moveX - x) * t);
                    this.currentY = (int) Math.round(y + (moveY - y) * t);
                } else if (phase < travelTime * 2) {
                    this.currentX = moveX;
                    this.currentY = moveY;
                } else if (phase < travelTime * 3) {
                    double t = (double) (phase - (travelTime * 2)) / travelTime;
                    this.currentX = (int) Math.round(moveX + (x - moveX) * t);
                    this.currentY = (int) Math.round(moveY + (y - moveY) * t);
                } else {
                    this.currentX = x;
                    this.currentY = y;
                }
            }
        }else if(this.move == movement.trigger){
            // int travelTime = Math.max(1, this.time);

            if (isTriggered) {
                // Check if a secondary post-trigger oscillation point has been assigned away from home
                boolean hasOscillatePath = (this.triggerMoveX != this.triggeredX && this.triggerMoveX != 0) || (this.triggerMoveY != this.triggeredY && this.triggerMoveY != 0);

                if (hasOscillatePath) {
                    // MULTI-STAGE: First slide from (x, y) to triggeredX/Y, then oscillate between triggeredX/Y and triggerMoveX/Y!
                    timeElapsed++;
                    int fullCycle = travelTime * 4;
                    int phase = timeElapsed % fullCycle;

                    if (phase < travelTime) {
                        double t = (double) phase / travelTime;
                        this.currentX = (int) (triggeredX + (triggerMoveX - triggeredX) * t);
                        this.currentY = (int) (triggeredY + (triggerMoveY - triggeredY) * t);
                    } else if (phase < travelTime * 2) {
                        this.currentX = triggerMoveX;
                        this.currentY = triggerMoveY;
                    } else if (phase < travelTime * 3) {
                        double t = (double) (phase - (travelTime * 2)) / travelTime;
                        this.currentX = (int) (triggerMoveX + (triggeredX - triggerMoveX) * t);
                        this.currentY = (int) (triggerMoveY + (triggeredY - triggerMoveY) * t);
                    } else {
                        this.currentX = triggeredX;
                        this.currentY = triggeredY;
                    }
                } else {
                    // Standard Single Move Target: Glide smoothly from base origin (x, y) to triggeredX/Y position
                    if (timeElapsed < travelTime) {
                        timeElapsed++;
                    }
                    double t = (double) timeElapsed / travelTime;
                    this.currentX = (int) (x + (triggeredX - x) * t);
                    this.currentY = (int) (y + (triggeredY - y) * t);
                }
            } else {
                // Trigger is off: Slide backward towards starting coordinates (x, y)
                if (timeElapsed > 0) {
                    timeElapsed--;
                }
                double t = (double) timeElapsed / travelTime;
                
                boolean hasOscillatePath = (this.triggerMoveX != this.triggeredX && this.triggerMoveX != 0) || (this.triggerMoveY != this.triggeredY && this.triggerMoveY != 0);
                
                if (hasOscillatePath) {
                    // Return tracking loop for post-trigger oscillating items back down to clean origins
                    double speed = 3.0;
                    if (currentX < x) currentX = (int) Math.min(currentX + speed, x);
                    else if (currentX > x) currentX = (int) Math.max(currentX - speed, x);
                    if (currentY < y) currentY = (int) Math.min(currentY + speed, y);
                    else if (currentY > y) currentY = (int) Math.max(currentY - speed, y);
                } else {
                    this.currentX = (int) (x + (triggeredX - x) * t);
                    this.currentY = (int) (y + (triggeredY - y) * t);
                }
            }
        }
        this.deltaX = this.currentX - this.lastX;
        this.deltaY = this.currentY - this.lastY;
    }
}