package com.driver.services;

import com.driver.models.Book;
import com.driver.models.Card;
import com.driver.models.Transaction;
import com.driver.models.TransactionStatus;
import com.driver.repositories.BookRepository;
import com.driver.repositories.CardRepository;
import com.driver.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    public int max_allowed_books;

    @Value("${books.max_allowed_days}")
    public int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    public int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        // If it fails: throw new Exception("Book is either unavailable or not present");
        //2. card is present and activated
        // If it fails: throw new Exception("Card is invalid");
        //3. number of books issued against the card is strictly less than max_allowed_books
        // If it fails: throw new Exception("Book limit has reached for this card");
        //If the transaction is successful, save the transaction to the list of transactions and return the id

        //Note that the error message should match exactly in all cases
        Book book=bookRepository5.findById(bookId).orElse(null);
        Card card=cardRepository5.findById(cardId).orElse(null);

        if(book==null)
            throw new Exception("Book is either unavailable or not present");

        if(card==null)
            throw new Exception("Card is invalid");

        if(card.getBooks().size()>max_allowed_books)
            throw new Exception("Book limit has reached for this card");

        Transaction transaction = Transaction.builder().
                card(card).
                book(book).
                isIssueOperation(true).
                transactionStatus(TransactionStatus.SUCCESSFUL).
                transactionDate(new Date()).build();

        transactionRepository5.save(transaction);

        return "Success"; //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId, TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well

        Book book=bookRepository5.findById(bookId).orElse(null);
        Card card=cardRepository5.findById(cardId).orElse(null);

        Transaction returnBookTransaction  = null;
        Date issueDate=transaction.getTransactionDate();
        Date returnDate=new Date();
        long timeDifference=returnDate.getTime()-issueDate.getTime();
        long dayDifference=timeDifference/(1000*60*60*24)%365;
        int fineAmount=0;
        if(dayDifference>getMax_allowed_days) {
            fineAmount=(int)(dayDifference-getMax_allowed_days)*transaction.getFineAmount();
        }

        returnBookTransaction = Transaction.builder().
                card(card).
                book(book).
                fineAmount(fineAmount).
                isIssueOperation(true).
                transactionStatus(TransactionStatus.SUCCESSFUL).
                transactionDate(new Date()).build();

        return returnBookTransaction; //return the transaction after updating all details
    }
}
