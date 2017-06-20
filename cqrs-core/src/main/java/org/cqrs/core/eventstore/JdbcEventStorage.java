package org.cqrs.core.eventstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.cqrs.core.DomainEvent;
import org.cqrs.core.EventSourcingAggregateRoot;
import org.cqrs.util.JdbcUtil;
import org.cqrs.util.SequenceUUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcEventStorage extends EventStorage<String> {

  static final Logger logger = LoggerFactory.getLogger(JdbcEventStorage.class);
  
  public PreparedStatement getStatement(String sql) throws SQLException {
    Connection connection = JdbcUtil.getConnection("jdbc:mysql://192.168.1.49:3306/cqrs?useUnicode=true", "pzedu_db_rw", "#V4@spSA^mm5");
    return connection.prepareStatement(sql);
  }
  
  @Override
  void appendEvent(DomainEvent event) {
    
    String sql = "INSERT INTO events(id,aggregate_id,timestamp,version,data) VALUES(?,?,?,?,?)";
    
    try {
      PreparedStatement stmt = getStatement(sql);
      stmt.setString(1, SequenceUUID.get().toString());
      stmt.setString(2, event.getAggregateRoot().getId());
      stmt.setLong(3, System.currentTimeMillis());
      stmt.setLong(4, event.getAggregateRoot().getVersion());
      stmt.setString(5, eventSerializer.serialize(event));
      
      JdbcUtil.execute(stmt);
      
    } catch (SQLException e) {
      throw new RuntimeException("Append event error, cause by:", e);
    }
  }

  @Override
  NavigableMap<Long, DomainEvent> readEvents(String aggregateId, long fromVersion) {
    
    NavigableMap<Long, DomainEvent> events = new TreeMap<>();
    
    String sql = "SELECT version,data FROM events WHERE aggregate_id=? AND version>=?";
    
    try {
      PreparedStatement stmt = getStatement(sql);
      stmt.setString(1, aggregateId);
      stmt.setLong(2, fromVersion);
      
      ResultSet resultSet = JdbcUtil.exexuteQuery(stmt);
      while (resultSet.next()) {
        Long version = resultSet.getLong("version");
        String data = resultSet.getString("data");
        events.put(version, eventSerializer.deserialize(data));
      }
      
    } catch (SQLException e) {
      throw new RuntimeException("Read event error, cause by:", e);
    }
    
    return events;
  }

  @Override
  void storeSnapshot(EventSourcingAggregateRoot root) {
    
  }

  @Override
  void deleteSnapshots(String aggregateId) {
    
  }

  @Override
  <T extends EventSourcingAggregateRoot> T getAggregateRootFromSnapshot(String aggregateId) {
    return null;
  }
}
