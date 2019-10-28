import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

    private ServerSocket server;
    private Socket clientOne, clientTwo;

    private ObjectOutputStream outToClientOne, outToClientTwo;
    private ObjectInputStream inFromClientOne, inFromClientTwo;

    private Thread gameThread;

    private BattleShipTable playerOneBoard, playerTwoBoard;
    private BattleShipTable playerOnePTable, playerTwoPTable;

    private int numberOfConnections, numberOfHitsFromPlayerOne, numberOfHitsFromPlayerTwo;

    private boolean playerOneTurn;

    public static void main(String[] args) throws IOException {
        Server battleshipServer = new Server();

        battleshipServer.server = new ServerSocket(ServerConstants.PORT);
        System.out.println("Server Booted");

        Socket playerOne = battleshipServer.server.accept();
        battleshipServer.addFirstPlayer(playerOne);

        battleshipServer.gameThread = new Thread(battleshipServer);
        battleshipServer.gameThread.start();

        Socket playerTwo = battleshipServer.server.accept();
        battleshipServer.addSecondPlayer(playerTwo);
    }

    public Server() {
        numberOfConnections = 0;
        numberOfHitsFromPlayerOne = 0;
        numberOfHitsFromPlayerTwo = 0;

        playerOneBoard = new BattleShipTable();
        playerTwoBoard = new BattleShipTable();
        playerOnePTable = new BattleShipTable();
        playerTwoPTable = new BattleShipTable();

        playerOneTurn = true;
    }

    @Override
    public void run() {
        waitForSecondPlayer();

        setUpBoard(ServerConstants.PLAYER_ONE);
        setUpBoard(ServerConstants.PLAYER_TWO);

        while(theGameIsNotOver()) {
            if (playerOneTurn) {
                makeMove(ServerConstants.PLAYER_ONE);
                sendBoards(ServerConstants.PLAYER_ONE);
            } else {
                makeMove(ServerConstants.PLAYER_TWO);
                sendBoards(ServerConstants.PLAYER_TWO);
            }
            switchTurns();
        }

        sendWinningMessage();
    }

    private void sendWinningMessage() {
        Message winningMessage = new Message("Player " + getWinner() + " has emerged victorious");
        winningMessage.setGameIsStillActive(false);
        winningMessage.setIsYourTurn(true);

        try {
            outToClientOne.writeObject(winningMessage);
            outToClientTwo.writeObject(winningMessage);
        }catch (Exception e) {}
    }

    private boolean theGameIsNotOver() {
        return numberOfHitsFromPlayerOne != BattleShipTable.NUMBER_OF_HITS_POSSIBLE &&
                numberOfHitsFromPlayerTwo != BattleShipTable.NUMBER_OF_HITS_POSSIBLE;
    }

    private int getWinner() {
        int winner = (numberOfHitsFromPlayerOne == BattleShipTable.NUMBER_OF_HITS_POSSIBLE)
                        ? ServerConstants.PLAYER_ONE : ServerConstants.PLAYER_TWO;

        return winner;
    }

    private void sendBoards(int player) {
        Message boardInfo = new Message();
        boardInfo.setMsg(numberOfHitsFromPlayerOne + " " +numberOfHitsFromPlayerTwo);
        boardInfo.setMsgType(Message.MSG_BOARD_INFO);

        try {
            if (player == ServerConstants.PLAYER_ONE) {
                boardInfo.Ftable = new BattleShipTable(playerOneBoard.table);
                boardInfo.Ptable = new BattleShipTable(playerOnePTable.table);

                outToClientOne.writeObject(boardInfo);
                outToClientOne.flush();
            } else {
                boardInfo.Ftable = new BattleShipTable(playerTwoBoard.table);
                boardInfo.Ptable = new BattleShipTable(playerTwoPTable.table);

                outToClientTwo.writeObject(boardInfo);
                outToClientTwo.flush();
            }
        }catch (Exception e) {}
    }

    private void addFirstPlayer(Socket firstPlayer) {
        try {
            this.clientOne = firstPlayer;
            outToClientOne = new ObjectOutputStream(clientOne.getOutputStream());
            inFromClientOne = new ObjectInputStream(clientOne.getInputStream());
            ++numberOfConnections;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addSecondPlayer(Socket secondPlayer) {
        try {
            this.clientTwo = secondPlayer;
            outToClientTwo = new ObjectOutputStream(clientTwo.getOutputStream());
            inFromClientTwo = new ObjectInputStream(clientTwo.getInputStream());
            ++numberOfConnections;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean twoPlayersHaveNotJoined() {
        return numberOfConnections != ServerConstants.TWO_CONNECTIONS;
    }

    private void waitForSecondPlayer() {
        while(twoPlayersHaveNotJoined()){
            System.out.print("");
        }
    }

    public void switchTurns() {
        playerOneTurn = !playerOneTurn;
    }

    private void setUpBoard(int player) {
        displayWaitingMessageForOtherPlayer(player);

        addAircraftCarriers(player);
        addSubmarines(player);
        addDestroyers(player);
    }

    private void displayWaitingMessageForOtherPlayer(int player) {
        ObjectOutputStream clientOutputStream = (player == ServerConstants.PLAYER_TWO) ? outToClientOne : outToClientTwo;

        try {
            Message waitingMessage = player == ServerConstants.PLAYER_ONE
                    ? new Message(ServerConstants.PLAYER_TWO_WAIT_MESSAGE) : new Message(ServerConstants.PLAYER_ONE_WAIT_MESSAGE);
            waitingMessage.setIsYourTurn(false);

            clientOutputStream.writeObject(waitingMessage);
            clientOutputStream.flush();
        }catch (Exception e) {}
    }

    private void addAircraftCarriers(int player) {
        ShipPosition aircraftOnePosition = getShipPosition(ServerConstants.AIRCRAFT, player);
        ShipPosition aircraftTwoPosition = getShipPosition(ServerConstants.AIRCRAFT, player);

        if(player == ServerConstants.PLAYER_ONE) {
            playerOneBoard.insertAirCarrier(aircraftOnePosition.getStartingSquare(), aircraftOnePosition.getEndingSquare());
            playerOneBoard.insertAirCarrier(aircraftTwoPosition.getStartingSquare(), aircraftTwoPosition.getEndingSquare());
        }else {
            playerTwoBoard.insertAirCarrier(aircraftOnePosition.getStartingSquare(), aircraftOnePosition.getEndingSquare());
            playerTwoBoard.insertAirCarrier(aircraftTwoPosition.getStartingSquare(), aircraftTwoPosition.getEndingSquare());
        }
    }

    private void addSubmarines(int player) {
        ShipPosition submarineOnePosition = getShipPosition(ServerConstants.SUBMARINE, player);
        ShipPosition submarineTwoPosition = getShipPosition(ServerConstants.SUBMARINE, player);

        if(player == ServerConstants.PLAYER_ONE) {
            playerOneBoard.insertSubmarine(submarineOnePosition.getStartingSquare());
            playerOneBoard.insertSubmarine(submarineTwoPosition.getStartingSquare());
        }else {
            playerTwoBoard.insertSubmarine(submarineOnePosition.getStartingSquare());
            playerTwoBoard.insertSubmarine(submarineTwoPosition.getStartingSquare());
        }
    }

    private void addDestroyers(int player) {
        ShipPosition destroyerOnePosition = getShipPosition(ServerConstants.DESTROYER, player);
        ShipPosition destroyerTwoPosition = getShipPosition(ServerConstants.DESTROYER, player);

        if(player == ServerConstants.PLAYER_ONE) {
            playerOneBoard.insertDestroyer(destroyerOnePosition.getStartingSquare(), destroyerOnePosition.getEndingSquare());
            playerOneBoard.insertDestroyer(destroyerTwoPosition.getStartingSquare(), destroyerTwoPosition.getEndingSquare());
        }else {
            playerTwoBoard.insertDestroyer(destroyerOnePosition.getStartingSquare(), destroyerOnePosition.getEndingSquare());
            playerTwoBoard.insertDestroyer(destroyerTwoPosition.getStartingSquare(), destroyerTwoPosition.getEndingSquare());
        }

    }

    private ShipPosition getShipPosition(String ship, int player) {
        String shipMessage = "Enter squares for " + ship;

        ObjectOutputStream clientOutputStream = getActiveClientOutputStream(player);
        ObjectInputStream clientInputStream = getActiveClientInputStream(player);

        String startingSquare = null;
        String endingSquare = null;

        ShipPosition aircraftPosition = null;

        try {
            Message shipPositionMessage = new Message(shipMessage + '\n' + "Enter starting square");
            shipPositionMessage.setIsYourTurn(true);

            clientOutputStream.writeObject(shipPositionMessage);
            clientOutputStream.flush();

            startingSquare = ((String) clientInputStream.readObject()).trim();

            if(!ship.equals(ServerConstants.SUBMARINE)) {
                shipPositionMessage = new Message("Enter ending square");
                shipPositionMessage.setIsYourTurn(true);

                clientOutputStream.writeObject(shipPositionMessage);
                clientOutputStream.flush();

                endingSquare = ((String) clientInputStream.readObject()).trim();
            }

            aircraftPosition = new ShipPosition(startingSquare, endingSquare);
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            return aircraftPosition;
        }
    }

    private void makeMove(int player) {
        ObjectOutputStream activeClientOutputStream = getActiveClientOutputStream(player);
        ObjectInputStream activeClientInputStream = getActiveClientInputStream(player);

        ObjectOutputStream inactiveClientOutputStream = getInactiveClientOutputStream(player);

        String squareToBomb = null;
        try {
            Message moveMessage = new Message(ServerConstants.MESSAGE_FOR_PLAYER_TURN);

            Message waitMessage = (player == ServerConstants.PLAYER_ONE)
                    ? new Message(ServerConstants.PLAYER_TWO_WAIT_MESSAGE) : new Message(ServerConstants.PLAYER_ONE_WAIT_MESSAGE);

            moveMessage.setIsYourTurn(true);
            waitMessage.setIsYourTurn(false);

            moveMessage.setGameIsStillActive(true);
            waitMessage.setGameIsStillActive(true);

            activeClientOutputStream.writeObject(moveMessage);
            activeClientOutputStream.flush();

            inactiveClientOutputStream.writeObject(waitMessage);
            inactiveClientOutputStream.flush();

            squareToBomb = ((String) activeClientInputStream.readObject()).trim();

            bombBoard(squareToBomb, player);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bombBoard(String squareToBomb, int player) {
        if(player == ServerConstants.PLAYER_ONE) {
            boolean theMoveWasAHit = playerTwoBoard.isHit(squareToBomb, false) && playerOnePTable.isHit(squareToBomb, true);

            String symbolToPutOnBoard = BattleShipTable.MISS_SYMBOL;

            if(theMoveWasAHit) {
                ++numberOfHitsFromPlayerOne;
                symbolToPutOnBoard = BattleShipTable.HIT_SYMBOL;
            }

            playerOnePTable.insertHit(squareToBomb, symbolToPutOnBoard);
        }else {
            String symbolToPutOnBoard = BattleShipTable.MISS_SYMBOL;
            boolean theMoveWasAHit = playerOneBoard.isHit(squareToBomb, false) && playerTwoPTable.isHit(squareToBomb, true);

            if(theMoveWasAHit) {
                ++numberOfHitsFromPlayerTwo;
                symbolToPutOnBoard = BattleShipTable.HIT_SYMBOL;
            }

            playerTwoPTable.insertHit(squareToBomb, symbolToPutOnBoard);
        }
    }

    private ObjectInputStream getActiveClientInputStream(int player) {
        ObjectInputStream clientInputStream = (player == ServerConstants.PLAYER_ONE) ? inFromClientOne : inFromClientTwo;

        return clientInputStream;
    }

    private ObjectOutputStream getActiveClientOutputStream(int player) {
        ObjectOutputStream clientOutputStream = (player == ServerConstants.PLAYER_ONE) ? outToClientOne : outToClientTwo;

        return clientOutputStream;
    }

    private ObjectOutputStream getInactiveClientOutputStream(int player) {
        ObjectOutputStream clientOutputStream = (player != ServerConstants.PLAYER_ONE) ? outToClientOne : outToClientTwo;

        return clientOutputStream;
    }
}