/*==============================================================*/
/* DBMS name:      MySQL 5.0                                    */
/* Created on:     2026/6/17 8:39:49                            */
/*==============================================================*/


drop table if exists CINEMAS;

drop table if exists HALLS;

drop table if exists MOVIES;

drop table if exists MOVIE_IMAGES;

drop table if exists NOTIFICATIONS;

drop table if exists ORDERS;

drop table if exists ORDER_SEATS;

drop table if exists PAYMENTS;

drop table if exists REVIEWS;

drop table if exists SEATS;

drop table if exists SHOWTIMES;

drop table if exists USERS;

/*==============================================================*/
/* Table: CINEMAS                                               */
/*==============================================================*/
create table CINEMAS
(
   cinemas_id           bigint not null,
   name                 varchar(30),
   address              varchar(50),
   phone                char(11),
   created_at           datetime,
   updatad_at           datetime,
   cinema_status        varchar(15) default 'open',
   longitude            decimal(10,7),
   latitude             decimal(10,7),
   business_hours       varchar(100),
   primary key (cinemas_id)
);

/*==============================================================*/
/* Table: HALLS                                                 */
/*==============================================================*/
create table HALLS
(
   hall_id              bigint not null,
   cinemas_id           bigint,
   name                 varchar(30),
   seat_rows            int,
   seat_cols            int,
   shall_type           char(10) default 'normal',
   primary key (hall_id)
);

/*==============================================================*/
/* Table: MOVIES                                                */
/*==============================================================*/
create table MOVIES
(
   movie_id             bigint not null,
   title                varchar(30),
   duration             int,
   date                 date,
   rating               int,
   description          text,
   created_at           datetime,
   updatad_at           datetime,
   movies_status        varchar(15) default 'upcoming',
   primary key (movie_id)
);

/*==============================================================*/
/* Table: MOVIE_IMAGES                                          */
/*==============================================================*/
create table MOVIE_IMAGES
(
   movie_id             bigint,
   movieImage_id        bigint,
   image_url            varchar(50),
   image_type           varchar(10) default 'poster',
   sort_order           int default 1,
   is_cover             smallint,
   created_at           datetime
);

/*==============================================================*/
/* Table: NOTIFICATIONS                                         */
/*==============================================================*/
create table NOTIFICATIONS
(
   notice_id            bigint not null,
   ID                   bigint,
   content              text,
   created_at           datetime,
   type                 varchar(10),
   title                varchar(30),
   primary key (notice_id)
);

/*==============================================================*/
/* Table: ORDERS                                                */
/*==============================================================*/
create table ORDERS
(
   order_id             bigint not null,
   showtime_id          bigint,
   review_id            bigint,
   ID                   bigint,
   total_amount         decimal(6,2),
   created_at           datetime,
   updatad_at           datetime,
   order_status         varchar(15) default 'pending',
   ×ùÎ»ÐòºÅ                 varchar(100),
   primary key (order_id)
);

/*==============================================================*/
/* Table: ORDER_SEATS                                           */
/*==============================================================*/
create table ORDER_SEATS
(
   order_id             bigint not null,
   seat_id              bigint not null,
   order_seat_id        char(10),
   price                decimal(6,2),
   primary key (order_id, seat_id)
);

/*==============================================================*/
/* Table: PAYMENTS                                              */
/*==============================================================*/
create table PAYMENTS
(
   payment_id           bigint not null,
   order_id             bigint,
   method               varchar(10),
   amount               decimal(6,2),
   created_at           datetime,
   pay_status           varchar(15),
   primary key (payment_id)
);

/*==============================================================*/
/* Table: REVIEWS                                               */
/*==============================================================*/
create table REVIEWS
(
   review_id            bigint not null,
   order_id             bigint,
   ID                   bigint,
   movie_id             bigint,
   rating               int,
   content              text,
   created              datetime,
   primary key (review_id)
);

/*==============================================================*/
/* Table: SEATS                                                 */
/*==============================================================*/
create table SEATS
(
   seat_id              bigint not null,
   hall_id              bigint,
   row_num              int,
   col_num              int,
   type                 varchar(10),
   created_at           datetime,
   updata_at            datetime,
   seat_status          varchar(15) default 'active',
   primary key (seat_id)
);

/*==============================================================*/
/* Table: SHOWTIMES                                             */
/*==============================================================*/
create table SHOWTIMES
(
   showtime_id          bigint not null,
   hall_id              bigint,
   movie_id             bigint,
   show_time            time,
   created_at           datetime,
   updata_at            datetime,
   show_data            date,
   showtime_status      varchar(15) default 'normal',
   showtime_price       decimal(6,2),
   primary key (showtime_id)
);

/*==============================================================*/
/* Table: USERS                                                 */
/*==============================================================*/
create table USERS
(
   ID                   bigint not null,
   username             varchar(20),
   password             varchar(20),
   phone                char(11),
   role                 varchar(10) default 'user',
   wallet_bal           decimal(6,2),
   created_at           datetime,
   updated_at           datetime,
   version              smallint,
   primary key (ID)
);

alter table HALLS add constraint FK_Relationship_2 foreign key (cinemas_id)
      references CINEMAS (cinemas_id) on delete restrict on update restrict;

alter table MOVIE_IMAGES add constraint FK_Relationship_1 foreign key (movie_id)
      references MOVIES (movie_id) on delete restrict on update restrict;

alter table NOTIFICATIONS add constraint FK_Relationship_19 foreign key (ID)
      references USERS (ID) on delete restrict on update restrict;

alter table ORDERS add constraint FK_Relationship_12 foreign key (ID)
      references USERS (ID) on delete restrict on update restrict;

alter table ORDERS add constraint FK_Relationship_13 foreign key (showtime_id)
      references SHOWTIMES (showtime_id) on delete restrict on update restrict;

alter table ORDERS add constraint FK_Relationship_17 foreign key (review_id)
      references REVIEWS (review_id) on delete restrict on update restrict;

alter table ORDER_SEATS add constraint FK_ORDER_SEATS foreign key (order_id)
      references ORDERS (order_id) on delete restrict on update restrict;

alter table ORDER_SEATS add constraint FK_ORDER_SEATS2 foreign key (seat_id)
      references SEATS (seat_id) on delete restrict on update restrict;

alter table PAYMENTS add constraint FK_Relationship_14 foreign key (order_id)
      references ORDERS (order_id) on delete restrict on update restrict;

alter table REVIEWS add constraint FK_Relationship_15 foreign key (movie_id)
      references MOVIES (movie_id) on delete restrict on update restrict;

alter table REVIEWS add constraint FK_Relationship_16 foreign key (ID)
      references USERS (ID) on delete restrict on update restrict;

alter table REVIEWS add constraint FK_Relationship_18 foreign key (order_id)
      references ORDERS (order_id) on delete restrict on update restrict;

alter table SEATS add constraint FK_Relationship_5 foreign key (hall_id)
      references HALLS (hall_id) on delete restrict on update restrict;

alter table SHOWTIMES add constraint FK_Relationship_3 foreign key (hall_id)
      references HALLS (hall_id) on delete restrict on update restrict;

alter table SHOWTIMES add constraint FK_Relationship_4 foreign key (movie_id)
      references MOVIES (movie_id) on delete restrict on update restrict;

