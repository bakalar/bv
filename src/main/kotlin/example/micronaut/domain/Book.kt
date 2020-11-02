package example.micronaut.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "book")
class Book {
    constructor() {}
    constructor(isbn: String?) {
        this.isbn = isbn
        isProcessed = false
    }

    @Id
    @Column(name = "isbn", nullable = false, unique = true, updatable = false)
    var isbn: String? = null

    @Column(name = "page_count", nullable = false)
    var pageCount: Int? = null

    @Column(name = "processed", nullable = false)
    var isProcessed = false
    override fun toString(): String {
        return "Book{" +
                "isbn=" + isbn +
                ", pageCount='" + pageCount + '\'' +
                '}'
    }
}