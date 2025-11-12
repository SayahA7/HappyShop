package ci553.happyshop.client.customer;

import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * The CustomerView is separated into two sections by a line :
 *
 * 1. Search Page – Always visible, allowing customers to browse and search for products.
 * 2. the second page – display either the Trolley Page or the Receipt Page
 *    depending on the current context. Only one of these is shown at a time.
 */

public class CustomerView  {
    public CustomerController cusController;
    public ListView<ci553.happyshop.catalogue.Product> obrLvProducts;

    private final int WIDTH = UIStyle.customerWinWidth;
    private final int HEIGHT = UIStyle.customerWinHeight;
    private final int COLUMN_WIDTH = WIDTH / 2 - 10;

    private HBox hbRoot; // Top-level layout manager
    private VBox vbTrolleyPage;  //vbTrolleyPage and vbReceiptPage will swap with each other when need
    private VBox vbReceiptPage;

    public Label noResults;

    TextField tfId; //for user input on the search page. Made accessible so it can be accessed or modified by CustomerModel
    TextField tfName; //for user input on the search page. Made accessible so it can be accessed by CustomerModel

    //four controllers needs updating when program going on
    private ImageView ivProduct; //image area in searchPage
    private Label lbProductInfo;//product text info in searchPage
    private TextArea taTrolley; //in trolley Page
    private TextArea taReceipt;//in receipt page

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;

    public void start(Stage window) {
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = CreateTrolleyPage();
        vbReceiptPage = createReceiptPage();

        // Create a divider line
        Line line = new Line(0, 0, 0, HEIGHT);
        line.setStrokeWidth(4);
        line.setStroke(Color.PINK);
        VBox lineContainer = new VBox(line);
        lineContainer.setPrefWidth(4); // Give it some space
        lineContainer.setAlignment(Pos.CENTER);

        hbRoot = new HBox(10, vbSearchPage, lineContainer, vbTrolleyPage); //initialize to show trolleyPage
        hbRoot.setAlignment(Pos.CENTER);
        hbRoot.setStyle(UIStyle.rootStyle);

        Scene scene = new Scene(hbRoot, WIDTH, HEIGHT);
        window.setScene(scene);
        window.setTitle("🛒 HappyShop Customer Client");
        WinPosManager.registerWindow(window,WIDTH,HEIGHT); //calculate position x and y for this window
        window.show();
        viewWindow=window;// Sets viewWindow to this window for future reference and management.
    }

    /**
     * Create the search page for the customers client
     *
     * This part allows the customers to search for products by either their product ID or name
     * Search text field and button are both connect to the controller
     * Then passed to the CustomerModel to be processed there
     * @return search page which contains input field, search button and result list
     */

    private VBox createSearchPage() {
        Label laTitle = new Label("Search by product ID/Name");
        laTitle.setStyle(UIStyle.labelTitleStyle);

        // Create the search text field
        tfId = new TextField();
        tfId.setPromptText("Enter product ID or name"); // Prompt the customer "Enter product ID or name"
        tfId.setStyle(UIStyle.textFiledStyle);
        // Pressing Enter in this text field also triggers a search
        tfId.setOnAction(actionEvent -> {
            try {
                cusController.doAction("Search"); // connect the search button and text field to the controller
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // add search bar with 🔍 button to allow customers to search bt product ID or name
        Button btnSearch = new Button("🔍");
        btnSearch.setStyle(UIStyle.buttonStyle);
        btnSearch.setOnAction(this::buttonClicked); // same as pressing Enter

        HBox hbSearch = new HBox(10, tfId, btnSearch);
        hbSearch.setAlignment(Pos.CENTER);

        Label laSearchSummary = new Label("Search Summary");
        laSearchSummary.setStyle(UIStyle.labelStyle);

        //Label is shown when no products are found
        noResults = new Label();
        noResults.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold;");
        noResults.setVisible(false);

        // add Add To Trolley button
        Button AddToTrolley = new Button("Add To Trolley");
        AddToTrolley.setStyle(UIStyle.greenFillBtnStyle);
        AddToTrolley.setOnAction(this::buttonClicked);

        HBox hbAddToTrolley = new HBox(10, AddToTrolley);
        hbAddToTrolley.setAlignment(Pos.CENTER);
        hbAddToTrolley.setPadding(new Insets(5));

        obrLvProducts = new ListView<>();
        obrLvProducts.setPrefHeight(200);
        obrLvProducts.setStyle(UIStyle.listViewStyle);

        /**
         * Changes how each product appears inside the search results list.
         * Shows both product image and details side by side.
         * Uses a ListCell with an ImageView for the product photo
         * and a label for product information.
         */
        obrLvProducts.setCellFactory(param -> new ListCell<ci553.happyshop.catalogue.Product>() {
            @Override
            protected void updateItem(ci553.happyshop.catalogue.Product product, boolean empty) {
                super.updateItem(product, empty);

                if (empty || product == null) {
                    setGraphic(null);
                    System.out.println("setCellFactory - empty item");
                } else {
                    String imageName = product.getProductImageName(); // Get image name (e.g. "0001.jpg")
                    String relativeImageUrl = ci553.happyshop.utility.StorageLocation.imageFolder + imageName;
                    // Get the full absolute path to the image
                    Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
                    String imageFullUri = imageFullPath.toUri().toString(); // Build the full image Uri

                    ImageView ivPro;
                    try {
                        ivPro = new ImageView(new Image(imageFullUri, 50,45, true,true)); // Attempt to load the product image
                    } catch (Exception e) {
                        // If loading fails, use a default image directly from the resources folder
                        ivPro = new ImageView(new Image("imageHolder.jpg",50,45,true,true)); // Directly load from resources
                    }

                    Label laProToString = new Label(product.toString()); // Create a label for product details
                    HBox hbox = new HBox(10, ivPro, laProToString); // Put ImageView and label in a horizontal layout
                    setGraphic(hbox);  // Set the whole row content
                }
            }
        });
        // laSearchSummary and noResults side by side
        HBox hbSummary = new HBox(10, laSearchSummary, noResults);
        hbSummary.setAlignment(Pos.CENTER_LEFT);

        VBox vbSearchResult = new VBox(5, hbAddToTrolley, hbSummary, obrLvProducts);

        VBox vbSearchPage = new VBox(10, laTitle, hbSearch, vbSearchResult);
        vbSearchPage.setPrefWidth(COLUMN_WIDTH-10);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 15px;");

        return vbSearchPage;
    }


    private VBox CreateTrolleyPage() {
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taTrolley = new TextArea();
        taTrolley.setEditable(false);
        taTrolley.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel,btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        vbTrolleyPage = new VBox(15, laPageTitle, taTrolley, hbBtns);
        vbTrolleyPage.setPrefWidth(COLUMN_WIDTH);
        vbTrolleyPage.setAlignment(Pos.TOP_CENTER);
        vbTrolleyPage.setStyle("-fx-padding: 15px;");
        return vbTrolleyPage;
    }

    private VBox createReceiptPage() {
        Label laPageTitle = new Label("Receipt");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taReceipt = new TextArea();
        taReceipt.setEditable(false);
        taReceipt.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCloseReceipt = new Button("OK & Close"); //btn for closing receipt and showing trolley page
        btnCloseReceipt.setStyle(UIStyle.buttonStyle);

        btnCloseReceipt.setOnAction(this::buttonClicked);

        vbReceiptPage = new VBox(15, laPageTitle, taReceipt, btnCloseReceipt);
        vbReceiptPage.setPrefWidth(COLUMN_WIDTH);
        vbReceiptPage.setAlignment(Pos.TOP_CENTER);
        vbReceiptPage.setStyle(UIStyle.rootStyleYellow);
        return vbReceiptPage;
    }

    /**
     * Takes button clicks from the CustomerView and sends then to the controller.
     * Depending on the button pressed the cusController's doAction calls the appropriate method from the model class.
     * @param event button click event by the user
     */

    private void buttonClicked(ActionEvent event) {
        try{
            Button btn = (Button)event.getSource();
            String action = btn.getText();

            if (action.equals("🔍")) action = "Search";
            if(action.equals("Add To Trolley")){
                showTrolleyOrReceiptPage(vbTrolleyPage); //ensure trolleyPage shows if the last customer did not close their receiptPage
            }
            if(action.equals("OK & Close")){
                showTrolleyOrReceiptPage(vbTrolleyPage);
            }
            cusController.doAction(action);
        }
        catch(SQLException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(String imageName, String searchResult, String trolley, String receipt) {
        if (ivProduct != null) {
            ivProduct.setImage(new Image(imageName));
        }
        if (lbProductInfo != null) {
            lbProductInfo.setText(searchResult);
        }
        if (taTrolley != null) {
            taTrolley.setText(trolley);
        }
        if (taReceipt != null && !receipt.equals("")) {
            showTrolleyOrReceiptPage(vbReceiptPage);
            taReceipt.setText(receipt);
        }
    }

    /**
     * Replaces the last child of hbRoot with the specified page.
     * The last child is either vbTrolleyPage or vbReceiptPage.
     * @param pageToShow vbTrolleyPage or vbReceiptPage.
     */
    private void showTrolleyOrReceiptPage(Node pageToShow) {
        int lastIndex = hbRoot.getChildren().size() - 1;
        if (lastIndex >= 0) {
            hbRoot.getChildren().set(lastIndex, pageToShow);
        }
    }

    WindowBounds getWindowBounds() {
        return new WindowBounds(viewWindow.getX(), viewWindow.getY(),
                  viewWindow.getWidth(), viewWindow.getHeight());
    }
}
