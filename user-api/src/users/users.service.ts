import { Injectable, ConflictException, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as bcrypt from 'bcryptjs';
import { User, UserDocument, UserRole, UserStatus } from './schemas/user.schema';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { EventEmitter2 } from '@nestjs/event-emitter';

@Injectable()
export class UsersService {
  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    private eventEmitter: EventEmitter2,
  ) {}

  async create(createUserDto: CreateUserDto): Promise<User> {
    // Check if email already exists
    const existingUser = await this.userModel.findOne({ email: createUserDto.email });
    if (existingUser) {
      throw new ConflictException('Email already exists');
    }

    // Hash password
    const saltRounds = 12;
    const hashedPassword = await bcrypt.hash(createUserDto.password, saltRounds);

    // Create user
    const user = new this.userModel({
      ...createUserDto,
      password: hashedPassword,
      role: createUserDto.role || UserRole.USER,
      status: UserStatus.ACTIVE,
    });

    const savedUser = await user.save();

    // Publish event
    this.eventEmitter.emit('user.registered', {
      userId: savedUser._id.toString(),
      email: savedUser.email,
      firstName: savedUser.firstName,
      lastName: savedUser.lastName,
      role: savedUser.role,
    });

    return savedUser;
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.userModel.findOne({ email }).exec();
  }

  async findById(id: string): Promise<User> {
    const user = await this.userModel.findById(id).exec();
    if (!user) {
      throw new NotFoundException('User not found');
    }
    return user;
  }

  async findAll(page = 0, size = 10, filters: any = {}): Promise<{ users: User[]; total: number }> {
    const query = this.buildFilterQuery(filters);
    const users = await this.userModel
      .find(query)
      .skip(page * size)
      .limit(size)
      .sort({ createdAt: -1 })
      .exec();

    const total = await this.userModel.countDocuments(query).exec();
    return { users, total };
  }

  async update(id: string, updateUserDto: UpdateUserDto): Promise<User> {
    const user = await this.userModel.findByIdAndUpdate(id, updateUserDto, { new: true }).exec();
    if (!user) {
      throw new NotFoundException('User not found');
    }

    // Publish event
    this.eventEmitter.emit('user.updated', {
      userId: id,
      updatedFields: updateUserDto,
    });

    return user;
  }

  async remove(id: string): Promise<void> {
    const user = await this.userModel.findByIdAndUpdate(
      id,
      { status: UserStatus.INACTIVE },
      { new: true }
    ).exec();

    if (!user) {
      throw new NotFoundException('User not found');
    }

    // Publish event
    this.eventEmitter.emit('user.deactivated', {
      userId: id,
    });
  }

  async validatePassword(plainPassword: string, hashedPassword: string): Promise<boolean> {
    return bcrypt.compare(plainPassword, hashedPassword);
  }

  async updateRefreshToken(id: string, refreshToken: string | null): Promise<void> {
    const hashedToken = refreshToken ? await bcrypt.hash(refreshToken, 12) : null;
    await this.userModel.findByIdAndUpdate(id, { refreshToken: hashedToken }).exec();
  }

  private buildFilterQuery(filters: any) {
    const query: any = {};

    if (filters.email) {
      query.email = { $regex: filters.email, $options: 'i' };
    }
    if (filters.role) {
      query.role = filters.role;
    }
    if (filters.status) {
      query.status = filters.status;
    }

    return query;
  }
}