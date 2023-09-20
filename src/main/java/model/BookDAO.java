package model;

import beans.Book;
import beans.BookCategory;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for:
 *     1. add new books.
 *     2. modify existing books.
 *     3. search for books.
 *     4. fetch books.
 */
public class BookDAO {

    /**
     * fetch constant number of books = count.
     * @param count
     * @return array of book beans.
     */
    public static ArrayList<Book> getBooks(Integer count, Integer offset)
    {
        String query = "SELECT * FROM Book LIMIT " + count + " OFFSET " + offset + ";";

        ArrayList<Book> matchedBooks = new ArrayList<>();
        try{
            ResultSet resultSet = ModelManager.getInstance().executeQuery(query);
            while (resultSet.next()) {
                matchedBooks.add(buildBook(resultSet));
            }
            resultSet.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
        return matchedBooks;

    }

    /**
     * returns all book's categories in the shop.
     * @return
     */
    public static ArrayList<String> getCategories()
    {
        String query = "SELECT * FROM Category;";

        ArrayList<String> categories = new ArrayList<>();
        try{
            ResultSet resultSet = ModelManager.getInstance().executeQuery(query);
            while(resultSet.next()){
                categories.add(resultSet.getString("name"));
            }
            resultSet.close();
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            return categories;
        }
    }

    static public ArrayList<String> getBookAuthors(Integer ISBN) {
        ArrayList<String> authors = new ArrayList<>();
        String query = "SELECT author_name FROM Author WHERE ISBN = " + ISBN + ";";
        ResultSet rs = null;
        try {
            rs = ModelManager.getInstance().executeQuery(query);
            while (rs.next()) {
                authors.add(rs.getString("author_name"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return authors;
    }

    /**
     * To add a new book to the online store.
     * @param newBook the new book we want to add into our store.
     */
    public static boolean addNewBook(@NotNull Book newBook) {
        /**
         * adding the new book.
         */
        String query = "INSERT INTO Book VALUES "
                + "( "
                + newBook.getISBN() + " , "
                + "'" + newBook.getTitle() + "'" + " , "
                + "'" + newBook.getPublisherName() + "'" + " , "
                + "'" + newBook.getPublicationYear() + "'" + " , "
                + "'" + newBook.getCategory() + "'" + " , "
                + newBook.getPrice() + " , "
                + newBook.getThreshold() + " , "
                + newBook.getNumberOfCopies()
                + " );" ;

        try {
            ModelManager.getInstance().executeQuery(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        /**
         * adding the book authors associated with the new book.
         */
        for(String authorName : newBook.getAuthorsNames()){
            query = "INSERT INTO Author VALUES "
                    + "( "
                    + "'" + authorName + "'" + " , "
                    + newBook.getISBN()
                    + " );" ;

            try {
                ModelManager.getInstance().executeQuery(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * For updating an existing book, the user first searches for the book then he does the required update.
     * For a given book, the user can update the quantity in stock when a copy or more of the book is sold.
     * The user cannot update the quantity of a book if this update will cause the quantity of a book in stock to be
     * negative.
     * @param updatedBook the modified book attributes
     */
    public static boolean modifyBook(@NotNull Book updatedBook, Integer oldISBN, ArrayList<String> newAuthors) {
        /**
        * delete the old ones and not exist in the updated newAuthors.
        */
        String new_author_names = "";
        for(String name : newAuthors) {
            if (new_author_names.isEmpty()) {
                new_author_names += "( " + "'" + name + "'";
            } else {
                new_author_names += " , " + "'" + name + "'";
            }
        }
        new_author_names += " )";

        String delete_author_query = "DELETE FROM Author WHERE ISBN = " + oldISBN +
                            " AND author_name NOT IN " + new_author_names + " ;" ;


        /**
        * select the old authors then detect the new ones to be inserted.
        */
        String select_author_query = "SELECT author_name FROM Author WHERE ISBN = " + oldISBN + " ;";
        try {
            System.out.println("delete_author_query: " + delete_author_query);
            System.out.println("select_author_query: " + select_author_query);
            ModelManager.getInstance().executeQuery(delete_author_query);
            ResultSet rs = ModelManager.getInstance().executeQuery(select_author_query);
            while (rs.next()) {
                newAuthors.remove(rs.getString("author_name"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        String new_author_values = "";
        for(String name : newAuthors){
            if(new_author_values.isEmpty()){
                new_author_values += "( '" + name + "' , " + oldISBN + " )";
            }
            else{
                new_author_values += " , " + "( '" + name + "' , " + oldISBN + " )";
            }
        }
        new_author_values += " ;";

        String insert_author_query = "INSERT INTO Author VALUES" + new_author_values;


        try {
            if (!new_author_values.equals(" ;")) {
                System.out.println("insert_author_query: " + insert_author_query);
                ModelManager.getInstance().executeQuery(insert_author_query);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }



        /**
        * update the existing book.
        */
        String query = "UPDATE Book SET "
                + "ISBN = " + updatedBook.getISBN() + " , "
                + "title = " + "'" + updatedBook.getTitle() + "'" + " , "
                + "publisher = " + "'" + updatedBook.getPublisherName() + "'" + " , "
                + "publication_year = " + "'" + updatedBook.getPublicationYear() + "'" + " , "
                + "category = " + "'" + updatedBook.getCategory().toString() + "'" + " , "
                + "price = " + updatedBook.getPrice() + " , "
                + "threshold = " + updatedBook.getThreshold() + " , "
                + "copies = " + updatedBook.getNumberOfCopies() + " "
                + "WHERE ISBN = " + oldISBN + ";" ;


        try {
            System.out.println("UPDATE Book query: " + query);
            ModelManager.getInstance().executeQuery(query);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * The user can search for a book by ISBN, and title. The user can search for books of a specific Category,
     * author or publisher.
     */


    /**
     * find only one book or nothing by searching by title.
     * @param title the title we search by
     * @return the matched book.
     */
    public static Book findByTitle(@NotNull String title) {
        String query = "SELECT * FROM Book "
                        + "WHERE TITLE = '" + title + "';";

        ResultSet resultSet = null;
        try {
            resultSet = ModelManager.getInstance().executeQuery(query);
            Book book = buildBook(resultSet);
            resultSet.close();
            return book;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * find only one book or nothing by searching by ISBN.
     * @param ISBN the ISBN we search by
     * @return the matched book.
     */
    public static Book findByISBN(Integer ISBN) {
        String query = "SELECT * FROM Book "
                + "WHERE ISBN = " + ISBN + ";";

        ResultSet resultSet = null;
        try {
            resultSet = ModelManager.getInstance().executeQuery(query);
            resultSet.next();
            Book book = buildBook(resultSet);
            resultSet.close();
            return book;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * find many books by searching by Author Name.
     * @param authorName the author we search by
     * @return the matched books.
     */
    public static ArrayList<Book> findByAuthor(@NotNull String authorName) {
        String query = "SELECT * FROM Book "
                + "WHERE Author = " + "'" + authorName + "'" + ";";

        ArrayList<Book> matchedBooks = new ArrayList<>();
        try{
            ResultSet resultSet = ModelManager.getInstance().executeQuery(query);
            while (resultSet.next()) {
                matchedBooks.add(buildBook(resultSet));
            }
            resultSet.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
        return matchedBooks;
    }

    /**
     * find many books by searching by category.
     * @param category the category class we search by
     * @param offset
     * @return the matched books.
     */
    public static ArrayList<Book> findByCategory(@NotNull BookCategory category, Integer offset) {
        String query = "SELECT * FROM Book "
                + "WHERE category = " + "'" + category.name() + "'"
                + " LIMIT " + ModelManager.getPagecount() + " OFFSET " + offset + ";";

        ArrayList<Book> matchedBooks = new ArrayList<>();
        try{
            ResultSet resultSet = ModelManager.getInstance().executeQuery(query);
            while (resultSet.next()) {
                matchedBooks.add(buildBook(resultSet));
            }
            resultSet.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
        return matchedBooks;
    }

    /**
     * find many books by searching by publisher Name.
     * @param publisherName the publisher we search by
     * @return the matched books.
     */
    public static ArrayList<Book> findByPublisher(@NotNull String publisherName) {
        String query = "SELECT * FROM Book "
                + "WHERE publisher = " + "'" + publisherName + "'" + ";";

        ArrayList<Book> matchedBooks = new ArrayList<>();
        try{
            ResultSet resultSet = ModelManager.getInstance().executeQuery(query);
            while (resultSet.next()) {
                matchedBooks.add(buildBook(resultSet));
            }
            resultSet.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
        return matchedBooks;
    }

    public static ArrayList<Book> findByPubYear(String year) {
        String query = "SELECT * FROM Book "
                + "WHERE publication_year = " + "'" + year + "'" + ";";

        ArrayList<Book> matchedBooks = new ArrayList<>();
        try{
            ResultSet resultSet = ModelManager.getInstance().executeQuery(query);
            while (resultSet.next()) {
                matchedBooks.add(buildBook(resultSet));
            }
            resultSet.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
        return matchedBooks;
    }

    /**
     * find many books by searching using optional attributes.
     * @return the matched books.
     */
    public static ArrayList<Book> find(Integer ISBN, String title, String publisherName, BookCategory category,
                                       ArrayList<String> authorName, String pub_year, Integer offset) {
        ArrayList<Book> matchedBooks = new ArrayList<>();
        Boolean whereClause = false;
        String query = "SELECT DISTINCT Book.* FROM (Book NATURAL JOIN Author) ";
        if (authorName != null && authorName.size() > 0) {
            String names_list = "";
            for(String name : authorName){
                if(names_list.isEmpty()){
                    names_list += "\"" + name.trim();
                }
                else {
                    names_list += "," + name.trim();
                }
            }
            names_list += "\"";
            query += "WHERE (FIND_IN_SET (author_name, " + names_list + "))";
            whereClause = true;
        }
        List<String> conditions = new ArrayList<String>();

        System.out.println(title);
        conditions.add(makeCondition("ISBN", ISBN));
        conditions.add(makeCondition("title", title));
        System.out.println("1: " + conditions.get(conditions.size() - 1));

        conditions.add(makeCondition("publisherName", publisherName));
        conditions.add(makeCondition("category", category));
        conditions.add(makeCondition("publication_year", pub_year));
        for(String cond : conditions){
            if(cond != null){
                if(whereClause){
                    query += " AND " + cond;
                }
                else{
                    query += " WHERE " + cond;
                    whereClause = true;
                }
            }
        }
        query += " LIMIT " + ModelManager.getPagecount() + " OFFSET " + (offset == null? 0 : offset) + ";";
        System.out.println(query);

        ArrayList<Book> matchedBooks = new ArrayList<>();
        try{
            ResultSet resultSet = ModelManager.getInstance().executeQuery(query);
            while (resultSet.next()) {
                matchedBooks.add(buildBook(resultSet));
            }
            resultSet.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
        return matchedBooks;
    }


    private static String makeCondition(String attributeName, Object attribute_value){
        if (attribute_value == null) {
            return null;
        }
        if (attribute_value instanceof Integer) {
            return attributeName + " = " + attribute_value.toString();
        } else {
            return attributeName + " = " + "'" + attribute_value.toString() + "'";
        }
    }

    /**
     * build book bean from result set row.
     * @param rs the result set after executing the query
     * @return the matched book if it exists
     */
    private static Book buildBook(@NotNull ResultSet rs) {
        Book book = new Book();
        try {
            book.setISBN(Integer.parseInt(rs.getString("ISBN")));
            book.setTitle(rs.getString("title"));
            book.setPublisherName(rs.getString("publisher"));
            book.setPublicationYear(rs.getString("publication_year"));
            book.setCategory(BookCategory.valueOf(rs.getString("category")));
            book.setPrice(Double.parseDouble(rs.getString("price")));
            book.setThreshold(Integer.parseInt(rs.getString("threshold")));
            book.setNumberOfCopies(Integer.parseInt(rs.getString("copies")));
            return book;
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
