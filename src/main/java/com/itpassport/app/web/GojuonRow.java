package com.itpassport.app.web;

import com.itpassport.app.entity.Term;
import java.util.List;

public record GojuonRow(String id, String label, List<Term> terms) {
}
