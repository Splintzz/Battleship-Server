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

    private boolean itIsPlayerOnesTurn;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(ServerConstants.PORT);

        while(true) {
            Server battleshipServer = new Server();

            battleshipServer.server = serverSocket;
            System.out.println(ServerConstants.SERVED_BOOTED_MESSAGE);

            Socket playerOne = battleshipServer.server.accept();
            battleshipServer.addFirstPlayer(playerOne);

            battleshipServer.gameThread = new Thread(battleshipServer);
            battleshipServer.gameThread.start();

            Socket playerTwo = battleshipServer.server.accept();
            battleshipServer.addSecondPlayer(playerTwo);
        }
    }

    public Server() {
        numberOfConnections = 0;
        numberOfHitsFromPlayerOne = 0;
        numberOfHitsFromPlayerTwo = 0;

        playerOneBoard = new BattleShipTable();
        playerTwoBoard = new BattleShipTable();
        playerOnePTable = new BattleShipTable();
        playerTwoPTable = new BattleShipTable();

        itIsPlayerOnesTurn = true;
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

    @Override
    public void run() {
        waitForTheSecondPlayer();

        setUpBoard(ServerConstants.PLAYER_ONE);
        setUpBoard(ServerConstants.PLAYER_TWO);

        while(theGameIsNotOver()) {
            if (itIsPlayerOnesTurn) {
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

    private void waitForTheSecondPlayer() {
        while(twoPlayersHaveNotJoined()){
            doNothing();
        }
    }

    private boolean twoPlayersHaveNotJoined() {
        return numberOfConnections != ServerConstants.TWO_CONNECTIONS;
    }

    private void doNothing() {
        System.out.print("");
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
            Message waitingMessage = (player == ServerConstants.PLAYER_ONE) ?
                    new Message(ServerConstants.PLAYER_TWO_WAIT_MESSAGE) : new Message(ServerConstants.PLAYER_ONE_WAIT_MESSAGE);
            waitingMessage.setIsYourTurn(ServerConstants.IS_NOT_YOUR_TURN);

            clientOutputStream.writeObject(waitingMessage);
            clientOutputStream.flush();
        }catch (Exception e) {
            e.printStackTrace();
        }
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
            shipPositionMessage.setIsYourTurn(ServerConstants.IS_YOUR_TURN);

            writeMessageToClient(clientOutputStream, shipPositionMessage);

            startingSquare = ((String) clientInputStream.readObject()).trim();

            if(!ship.equals(ServerConstants.SUBMARINE)) {
                shipPositionMessage = new Message("Enter ending square");
                shipPositionMessage.setIsYourTurn(ServerConstants.IS_YOUR_TURN);

                writeMessageToClient(clientOutputStream, shipPositionMessage);

                endingSquare = ((String) clientInputStream.readObject()).trim();
            }

            aircraftPosition = new ShipPosition(startingSquare, endingSquare);
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            return aircraftPosition;
        }
    }

    private boolean theGameIsNotOver() {
        return numberOfHitsFromPlayerOne != BattleShipTable.NUMBER_OF_HITS_POSSIBLE &&
                numberOfHitsFromPlayerTwo != BattleShipTable.NUMBER_OF_HITS_POSSIBLE;
    }

    public void switchTurns() {
        itIsPlayerOnesTurn = !itIsPlayerOnesTurn;
    }

    private void makeMove(int player) {
        ObjectOutputStream activeClientOutputStream = getActiveClientOutputStream(player);
        ObjectInputStream activeClientInputStream = getActiveClientInputStream(player);

        ObjectOutputStream inactiveClientOutputStream = getInactiveClientOutputStream(player);

        String squareToBomb = null;

        Message moveMessage = new Message(ServerConstants.MESSAGE_FOR_PLAYER_TURN);

        Message waitMessage = (player == ServerConstants.PLAYER_ONE) ?
                new Message(ServerConstants.PLAYER_TWO_WAIT_MESSAGE) : new Message(ServerConstants.PLAYER_ONE_WAIT_MESSAGE);

        moveMessage.setIsYourTurn(ServerConstants.IS_YOUR_TURN);
        waitMessage.setIsYourTurn(ServerConstants.IS_NOT_YOUR_TURN);

        moveMessage.setGameIsStillActive(ServerConstants.ACTIVE);
        waitMessage.setGameIsStillActive(ServerConstants.ACTIVE);

        writeMessageToClient(activeClientOutputStream, moveMessage);
        writeMessageToClient(inactiveClientOutputStream, waitMessage);

        try {
            squareToBomb = ((String) activeClientInputStream.readObject()).trim();
        }catch (Exception e) {
            e.printStackTrace();
        }

        bombBoard(squareToBomb, player);
    }

    private void bombBoard(String squareToBomb, int player) {
        boolean isPTable = true;

        if(player == ServerConstants.PLAYER_ONE) {
            boolean theMoveWasAHit = playerTwoBoard.isHit(squareToBomb, !isPTable) &&
                                     playerOnePTable.isHit(squareToBomb, isPTable);

            String symbolToPutOnBoard = BattleShipTable.MISS_SYMBOL;

            if(theMoveWasAHit) {
                ++numberOfHitsFromPlayerOne;
                symbolToPutOnBoard = BattleShipTable.HIT_SYMBOL;
                playerTwoBoard.insertHitToBoard(squareToBomb, symbolToPutOnBoard);
            }

            playerOnePTable.insertHit(squareToBomb, symbolToPutOnBoard);

        }else {
            String symbolToPutOnBoard = BattleShipTable.MISS_SYMBOL;
            boolean theMoveWasAHit = playerOneBoard.isHit(squareToBomb, !isPTable) &&
                                     playerTwoPTable.isHit(squareToBomb, isPTable);

            if(theMoveWasAHit) {
                ++numberOfHitsFromPlayerTwo;
                symbolToPutOnBoard = BattleShipTable.HIT_SYMBOL;
                playerOneBoard.insertHitToBoard(squareToBomb, symbolToPutOnBoard);
            }

            playerTwoPTable.insertHit(squareToBomb, symbolToPutOnBoard);
        }
    }

    private void sendBoards(int player) {
        Message boardInfo = new Message();

        boardInfo.setMsg(numberOfHitsFromPlayerOne + " " +numberOfHitsFromPlayerTwo);

        if (player == ServerConstants.PLAYER_ONE) {
            boardInfo.Ftable = new BattleShipTable(playerOneBoard.table);
            boardInfo.Ptable = new BattleShipTable(playerOnePTable.table);

            writeMessageToClient(outToClientOne, boardInfo);

        } else {
            boardInfo.Ftable = new BattleShipTable(playerTwoBoard.table);
            boardInfo.Ptable = new BattleShipTable(playerTwoPTable.table);

            writeMessageToClient(outToClientTwo, boardInfo);
        }
    }

    private void sendWinningMessage() {
        Message winningMessage = new Message("Player " + getWinner() + " has emerged victorious");

        winningMessage.setGameIsStillActive(ServerConstants.INACTIVE);
        winningMessage.setIsYourTurn(ServerConstants.IS_YOUR_TURN);

        writeMessageToClient(outToClientOne, winningMessage);
        writeMessageToClient(outToClientTwo, winningMessage);
    }

    private int getWinner() {
        int winner = (numberOfHitsFromPlayerOne == BattleShipTable.NUMBER_OF_HITS_POSSIBLE) ?
                ServerConstants.PLAYER_ONE : ServerConstants.PLAYER_TWO;

        return winner;
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

    private void writeMessageToClient(ObjectOutputStream outToClient, Message outgoingMessage) {
        try {
            outToClient.writeObject(outgoingMessage);
            outToClient.flush();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}