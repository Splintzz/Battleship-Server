public class ShipPosition {
    private String startingSquare;
    private String endingSquare;

    public ShipPosition(String startingSquare, String endingSquare) {
        this.startingSquare = startingSquare;
        this.endingSquare = endingSquare;
    }

    public String getStartingSquare() {
        return startingSquare;
    }

    public void setStartingSquare(String startingSquare) {
        this.startingSquare = startingSquare;
    }

    public String getEndingSquare() {
        return endingSquare;
    }

    public void setEndingSquare(String endingSquare) {
        this.endingSquare = endingSquare;
    }
}
